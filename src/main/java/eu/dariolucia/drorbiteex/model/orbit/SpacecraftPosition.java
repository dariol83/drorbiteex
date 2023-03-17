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

package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.TimeScalesFactory;

import java.util.Date;

public final class SpacecraftPosition {

    private final Date time;

    private final Orbit orbit;

    private final int orbitNumber;

    private final SpacecraftState spacecraftState;

    private final Vector3D positionVector;

    private final GeodeticPoint latLonHeight;

    public SpacecraftPosition(Orbit orbit, int orbitNumber, SpacecraftState spacecraftState) {
        this.orbit = orbit;
        this.orbitNumber = orbitNumber;
        this.spacecraftState = spacecraftState;
        this.positionVector = spacecraftState.getPVCoordinates(EarthReferenceUtils.getITRF()).getPosition();
        this.latLonHeight = EarthReferenceUtils.cartesianToGeodetic(this.positionVector, this.spacecraftState.getDate());
        this.time = spacecraftState.getDate().toDate(TimeScalesFactory.getUTC());
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public int getOrbitNumber() {
        return orbitNumber;
    }

    public SpacecraftState getSpacecraftState() {
        return spacecraftState;
    }

    public Vector3D getPositionVector() {
        return positionVector;
    }

    public GeodeticPoint getLatLonHeight() {
        return latLonHeight;
    }

    public Date getTime() {
        return time;
    }

    public Vector3D computeVisibilityVectorFrom(Vector3D poiPositionVector) {
        return new Vector3D(
                positionVector.getX() - poiPositionVector.getX(),
                positionVector.getY() - poiPositionVector.getY(),
                positionVector.getZ() - poiPositionVector.getZ()).normalize();
    }
}
