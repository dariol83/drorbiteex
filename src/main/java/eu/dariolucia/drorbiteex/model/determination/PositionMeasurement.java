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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;

import java.time.Instant;

public class PositionMeasurement extends Measurement {

    private final Vector3D position; // in meters, the measurement must be in the orbit propagation frame

    public PositionMeasurement(Instant time, Vector3D position) {
        super(time);
        this.position = position;
    }

    public Vector3D getPosition() {
        return position;
    }

    @Override
    public String getInfo() {
        return "Position: " + position + " ";
    }

    @Override
    public Type getType() {
        return Type.POSITION;
    }

    @Override
    public AbstractMeasurement<?> toOrekitMeasurement(ObservableSatellite satellite) {
        return new Position(getAbsoluteDate(), position, 0.2, 1.0, satellite);
    }
}
