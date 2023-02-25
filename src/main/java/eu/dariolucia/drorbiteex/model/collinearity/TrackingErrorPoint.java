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

package eu.dariolucia.drorbiteex.model.collinearity;

import java.time.Instant;

public class TrackingErrorPoint {

    public static TrackingErrorPoint noVisibility(Instant time) {
        return new TrackingErrorPoint(time, -1, -1);
    }

    private final Instant time;
    private final double azimuthError;
    private final double elevationError;

    public TrackingErrorPoint(Instant time, double azimuthError, double elevationError) {
        this.time = time;
        this.azimuthError = azimuthError;
        this.elevationError = elevationError;
    }

    public Instant getTime() {
        return time;
    }

    public double getAzimuthError() {
        return azimuthError;
    }

    public double getElevationError() {
        return elevationError;
    }
}
