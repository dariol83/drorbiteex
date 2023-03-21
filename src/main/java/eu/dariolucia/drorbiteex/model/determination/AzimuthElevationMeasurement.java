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

import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.Frame;

import java.time.Instant;
import java.util.Date;

public class AzimuthElevationMeasurement extends Measurement {

    private final GroundStation station;
    private final double azimuth; // in degrees
    private final double elevation; // in degrees

    public AzimuthElevationMeasurement(Instant time, GroundStation station, double azimuth, double elevation) {
        super(time);
        this.station = station;
        this.azimuth = azimuth;
        this.elevation = elevation;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getElevation() {
        return elevation;
    }

    public GroundStation getStation() {
        return station;
    }

    @Override
    public String getInfo() {
        return "Ground Station: " + station.getName() + " - Azimuth: " + azimuth + " deg - Elevation: " + elevation + " deg";
    }

    @Override
    public Type getType() {
        return Type.AZ_EL;
    }

    @Override
    public AbstractMeasurement<?> toOrekitMeasurement(ObservableSatellite satellite, Frame orbitFrame) {
        return new AngularAzEl(getStation().toOrekitGroundStation(), getAbsoluteDate(),
                new double[] {Math.toRadians(azimuth), Math.toRadians(elevation)}, new double[] {0.2, 0.2}, new double[] {1.0, 1.0}, satellite);
    }
}
