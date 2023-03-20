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
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;

import java.time.Instant;
import java.util.Date;

public class RangeMeasurement extends Measurement {

    private final GroundStation station;
    private final double range; // in seconds

    public RangeMeasurement(Instant time, GroundStation station, double range) {
        super(time);
        this.range = range;
        this.station = station;
    }

    public double getRange() {
        return range;
    }

    public GroundStation getStation() {
        return station;
    }

    @Override
    public String getInfo() {
        return "Ground Station: " + station.getName() + " - Range: " + range + " s";
    }

    @Override
    public Type getType() {
        return Type.RANGE;
    }

    @Override
    public AbstractMeasurement<?> toOrekitMeasurement(ObservableSatellite satellite) {
        return new Range(getStation().toOrekitGroundStation(),
                true,
                getAbsoluteDate(),
                range, 0.2, 1.0,
                satellite);
    }
}
