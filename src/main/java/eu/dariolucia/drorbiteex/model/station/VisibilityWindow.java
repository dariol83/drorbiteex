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

package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.geometry.Point2D;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import javax.sound.midi.Track;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VisibilityWindow implements Comparable<VisibilityWindow> {

    private final Orbit orbit;
    private final int orbitNumber;
    private final Date aos;
    private final Date los;
    private final GroundStation station;
    private final List<TrackPoint> azimuthElevationTrack = new LinkedList<>();

    private final UUID id;

    VisibilityWindow(Orbit orbit, int orbitNumber, Date aos, Date los, GroundStation station) {
        this(orbit, orbitNumber, aos, los, station, Collections.emptyList());
    }

    VisibilityWindow(Orbit orbit, int orbitNumber, Date aos, Date los, GroundStation station, List<TrackPoint> initialTrackPoints) {
        this.id = UUID.randomUUID();
        this.orbitNumber = orbitNumber;
        this.aos = aos;
        this.los = los;
        this.station = station;
        this.orbit = orbit;
        this.azimuthElevationTrack.addAll(initialTrackPoints);
    }

    public UUID getId() {
        return id;
    }

    void initialiseGroundTrack(Orbit orbit, Propagator propagator) {
        if(this.azimuthElevationTrack.isEmpty() && propagator != null) {
            Date[] startEndTrackDates = deriveDates();
            if(startEndTrackDates == null) {
                // Ground track is empty
                return;
            }
            Date currentDate = startEndTrackDates[0];
            Date endDate = startEndTrackDates[1];
            // Start propagation from currentDate to endDate: 10 seconds interval
            while (currentDate.before(endDate)) {
                SpacecraftState next = propagator.propagate(new AbsoluteDate(currentDate, TimeScalesFactory.getUTC()));
                // Convert spacecraft point to azimuth/elevation
                double[] azElPoint = this.station.getAzimuthElevationOf(next);
                // If the elevation is below 0, discard the point
                if(azElPoint[1] >= 0) {
                    this.azimuthElevationTrack.add(new TrackPoint(currentDate, new SpacecraftPosition(orbit, this.orbitNumber, next), this.station, azElPoint[0], azElPoint[1]));
                }
                // Next point, XXX seconds after (check configuration)
                currentDate = new Date(currentDate.getTime() + station.getConfiguration().getTrackingInterval() * 1000L);
            }
            // End date
            SpacecraftState next = propagator.propagate(new AbsoluteDate(endDate, TimeScalesFactory.getUTC()));
            // Convert spacecraft point to azimuth/elevation
            double[] azElPoint = this.station.getAzimuthElevationOf(next);
            //
            if(azElPoint[1] >= 0) {
                this.azimuthElevationTrack.add(new TrackPoint(currentDate, new SpacecraftPosition(orbit, this.orbitNumber, next), this.station, azElPoint[0], azElPoint[1]));
            }
        }
    }

    private Date[] deriveDates() {
        if(aos != null && los != null) {
            return new Date[] { aos, los };
        } else if(aos == null && los == null) {
            return null;
        } else if(aos == null) { // los != null // If you don't have AOS, then use LOS - 20 minutes
            return new Date[] { new Date(los.getTime() - 20 * 60 * 1000), los };
        } else { // los == null // If you don't have LOS, then use AOS + 20 minutes
            return new Date[] { aos, new Date(aos.getTime() + 20 * 60 * 1000) };
        }
    }

    public int getOrbitNumber() {
        return orbitNumber;
    }

    public Date getAos() {
        return aos;
    }

    public Date getLos() {
        return los;
    }

    public GroundStation getStation() {
        return station;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    @Override
    public String toString() {
        return "VisibilityWindow{" +
                "orbit='" + orbit + '\'' +
                ", orbitNumber=" + orbitNumber +
                ", aos=" + aos +
                ", los=" + los +
                ", station=" + station +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisibilityWindow that = (VisibilityWindow) o;
        return orbitNumber == that.orbitNumber && Objects.equals(orbit, that.orbit) && Objects.equals(aos, that.aos) && Objects.equals(los, that.los) && Objects.equals(station, that.station);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orbit, orbitNumber, aos, los, station);
    }

    @Override
    public int compareTo(VisibilityWindow o) {
        if(this.aos == null && o.aos == null) {
            if(this.los == null && o.los == null) {
                return this.orbit.getCode().compareTo(o.getOrbit().getCode());
            } else if(this.los == null) {
                return -1;
            } else if(o.los == null) {
                return 1;
            } else {
                return this.los.compareTo(o.los);
            }
        } else if(this.aos == null) {
            return -1;
        } else if(o.aos == null) {
            return 1;
        } else {
            return this.aos.compareTo(o.aos);
        }
    }

    public String getAosString() {
        if(this.aos == null) {
            return "---";
        } else {
            return TimeUtils.formatDate(this.aos);
        }
    }

    public String getLosString() {
        if(this.los == null) {
            return "---";
        } else {
            return TimeUtils.formatDate(this.los);
        }
    }

    public List<TrackPoint> getGroundTrack() {
        return Collections.unmodifiableList(this.azimuthElevationTrack);
    }

    public boolean isInPass(Date time) {
        if(this.aos == null && this.los == null) {
            return true;
        } else if(this.aos == null) {
            return time.before(this.los);
        } else if(this.los == null) {
            return time.after(this.aos);
        } else {
            return time.after(this.aos) && time.before(this.los);
        }
    }

    public void exportVisibilityInfoTo(OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        // CVS format: gs code, orbit code, orbit number, AOS, AOS EL, AOS AZ, LOS, LOS EL, LOS AZ
        sb.append(station.getCode().replace(',', '_')).append(",");
        sb.append(orbit.getCode().replace(',', '_')).append(",");
        sb.append(orbitNumber).append(",");
        sb.append(TimeUtils.formatDate(getGroundTrack().get(0).getTime())).append(",");
        sb.append(getGroundTrack().get(0).getElevation()).append(",");
        sb.append(getGroundTrack().get(0).getAzimuth()).append(",");
        sb.append(TimeUtils.formatDate(getGroundTrack().get(getGroundTrack().size() - 1).getTime())).append(",");
        sb.append(getGroundTrack().get(getGroundTrack().size() - 1).getElevation()).append(",");
        sb.append(getGroundTrack().get(getGroundTrack().size() - 1).getAzimuth());
        sb.append("\n");
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void exportGroundTrackingInfoTo(OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        // CVS format: gs code, orbit code, orbit number, Time, EL, AZ, doppler
        for(int i = 0; i < getGroundTrack().size(); ++i) {
            sb.append(station.getCode().replace(',', '_')).append(",");
            sb.append(orbit.getCode().replace(',', '_')).append(",");
            sb.append(orbitNumber).append(",");
            sb.append(TimeUtils.formatDate(getGroundTrack().get(i).getTime())).append(",");
            sb.append(getGroundTrack().get(i).getElevation()).append(",");
            sb.append(getGroundTrack().get(i).getAzimuth()).append(",");
            sb.append(getGroundTrack().get(i).getRange()).append(",");
            sb.append(getGroundTrack().get(i).getDoppler());
            sb.append("\n");
        }
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public TrackPoint getMaxElevationPoint() {
        TrackPoint maxElPoint = null;
        for (TrackPoint tp : getGroundTrack()) {
            if (maxElPoint == null || maxElPoint.getElevation() < tp.getElevation()) {
                maxElPoint = tp;
            }
        }
        return maxElPoint;
    }
}
