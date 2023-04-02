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
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;

import java.time.Instant;

public class PositionMeasurement extends Measurement {

    private final Vector3D position; // in meters, the measurement must be in the orbit propagation frame
    private final Vector3D velocity; // in meter/sec, can be null, the measurement must be in the orbit propagation frame
    private final Frame referenceFrame; // reference frame of the position

    public PositionMeasurement(Instant time, Vector3D position, Vector3D velocity, Frame referenceFrame) {
        super(time);
        if(position == null) {
            throw new NullPointerException("Argument 'position' is null");
        }
        if(referenceFrame == null) {
            throw new NullPointerException("Argument 'referenceFrame' is null");
        }
        this.position = position;
        this.velocity = velocity;
        this.referenceFrame = referenceFrame;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Vector3D getVelocity() {
        return velocity;
    }

    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    @Override
    public String getInfo() {
        if(velocity != null) {
            return "Position: " + position + " - Velocity: " + velocity + " - Ref: " + referenceFrame;
        } else {
            return "Position: " + position + " - Ref: " + referenceFrame;
        }
    }

    @Override
    public Type getType() {
        return Type.POSITION;
    }

    @Override
    public AbstractMeasurement<?> toOrekitMeasurement(ObservableSatellite satellite, Frame orbitFrame) {
        Vector3D newPosition = referenceFrame.getTransformTo(orbitFrame, getAbsoluteDate()).transformPosition(getPosition());
        if(velocity != null) {
            Vector3D newVelocity = referenceFrame.getTransformTo(orbitFrame, getAbsoluteDate()).transformVector(velocity);
            return new PV(getAbsoluteDate(), newPosition, newVelocity, 0.1, 0.1, 1.0, satellite);
        } else {
            return new Position(getAbsoluteDate(), newPosition, 0.1, 1.0, satellite);
        }
    }
}
