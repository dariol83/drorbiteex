/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;

import java.util.Date;

public class TrackPoint {

    private final Date time;
    private final int orbitNumber;
    private final SpacecraftPosition spacecraftPosition;
    private final GroundStation station;
    private final double elevation;
    private final double azimuth;

    public TrackPoint(Date time, SpacecraftPosition spacecraftPosition, GroundStation station, double azimuth, double elevation) {
        this.time = time;
        this.orbitNumber = spacecraftPosition.getOrbitNumber();
        this.spacecraftPosition = spacecraftPosition;
        this.station = station;
        this.elevation = elevation;
        this.azimuth = azimuth;
    }

    public Date getTime() {
        return time;
    }

    public int getOrbitNumber() {
        return orbitNumber;
    }

    public SpacecraftPosition getSpacecraftPosition() {
        return spacecraftPosition;
    }

    public GroundStation getStation() {
        return station;
    }

    public double getElevation() {
        return elevation;
    }

    public double getAzimuth() {
        return azimuth;
    }
}
