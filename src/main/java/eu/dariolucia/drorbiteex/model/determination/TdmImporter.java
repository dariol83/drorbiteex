/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.model.determination;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.tdm.*;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.time.TimeScalesFactory;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TdmImporter {
    public static List<Measurement> load(String absolutePath, Orbit satellite, List<GroundStation> stations) {
        List<Measurement> measurements = new LinkedList<>();
        TdmParser parser = new ParserBuilder().buildTdmParser();
        Tdm tdmObject = parser.parseMessage(new DataSource("tdm", () -> new FileInputStream(absolutePath)));
        for(Segment<TdmMetadata, ObservationsBlock> segment : tdmObject.getSegments()) {
            measurements.addAll(convertSegment(segment, satellite, stations));
        }
        return measurements;
    }

    private static List<Measurement> convertSegment(Segment<TdmMetadata, ObservationsBlock> segment, Orbit satellite, List<GroundStation> stations) {
        if(!segment.getMetadata().getParticipants().containsValue(satellite.getCode())) {
            // TODO: log somehow
            return Collections.emptyList();
        }
        if(segment.getMetadata().getAngleType() != AngleType.AZEL) {
            // TODO: log somehow
            return Collections.emptyList();
        }
        // Locate ground station using the PARTICIPANT_1
        String gsCode = segment.getMetadata().getParticipants().get(1);
        Optional<GroundStation> gs = stations.stream().filter(o -> o.getCode().equals(gsCode)).findFirst();
        GroundStation station = gs.orElse(null);
        if(station == null) {
            // TODO: log somehow
            return Collections.emptyList();
        }
        // First scan, look for ANGLE_1 (AZ) and ANGLE_2 (EL) and build a correspondence for a given time
        Map<Instant, MeasurementCollector> epoch2data = new LinkedHashMap<>();
        for(Observation observation : segment.getData().getObservations()) {
            Instant i = getEpoch(observation);
            MeasurementCollector mc = epoch2data.computeIfAbsent(i, MeasurementCollector::new);
            if(observation.getType() == ObservationType.ANGLE_1) {
                mc.setAzimuth(observation.getMeasurement());
            } else if(observation.getType() == ObservationType.ANGLE_2) {
                mc.setElevation(observation.getMeasurement());
            } else if(observation.getType() == ObservationType.RANGE) {
                mc.setRange(observation.getMeasurement());
            }
        }
        // Move map to a list
        List<MeasurementCollector> list = new ArrayList<>(epoch2data.values());
        Collections.sort(list);
        return list.stream().map(o -> o.toMeasurements(station)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static Instant getEpoch(Observation observation) {
        return observation.getEpoch().toDate(TimeScalesFactory.getUTC()).toInstant();
    }

    private static class MeasurementCollector implements Comparable<MeasurementCollector> {
        private final Instant time;

        private double azimuth = -1; // in degrees

        private double elevation = -1; // in degrees

        private double range = -1; // in seconds

        public MeasurementCollector(Instant time) {
            this.time = time;
        }

        public Instant getTime() {
            return time;
        }

        public double getAzimuth() {
            return azimuth;
        }

        public void setAzimuth(double azimuth) {
            this.azimuth = azimuth;
        }

        public double getElevation() {
            return elevation;
        }

        public void setElevation(double elevation) {
            this.elevation = elevation;
        }

        public double getRange() {
            return range;
        }

        public void setRange(double range) {
            this.range = range;
        }

        @Override
        public int compareTo(MeasurementCollector o) {
            int timeResult = getTime().compareTo(o.getTime());
            if(timeResult == 0) {
                timeResult = Double.compare(getAzimuth(), o.getAzimuth());
                if(timeResult == 0) {
                    timeResult = Double.compare(getElevation(), o.getElevation());
                    if(timeResult == 0) {
                        timeResult = Double.compare(getRange(), o.getRange());
                    }
                }
            }
            return timeResult;
        }

        public List<Measurement> toMeasurements(GroundStation station) {
            List<Measurement> measurements = new LinkedList<>();
            if(range != -1) {
                measurements.add(new RangeMeasurement(time, station, range));
            }
            if(azimuth != -1 && elevation != -1) {
                measurements.add(new AzimuthElevationMeasurement(time, station, azimuth, elevation));
            }
            return measurements;
        }
    }
}
