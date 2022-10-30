package eu.dariolucia.drorbiteex.model;

import eu.dariolucia.drorbiteex.data.Utils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.Date;

public final class SpacecraftPosition {

    private final Orbit orbit;

    private final SpacecraftState spacecraftState;

    private final Vector3D positionVector;

    private final GeodeticPoint latLonHeight;

    public SpacecraftPosition(Orbit orbit, SpacecraftState spacecraftState) {
        this.orbit = orbit;
        this.spacecraftState = spacecraftState;
        this.positionVector = spacecraftState.getPVCoordinates(Utils.ITRF).getPosition();
        this.latLonHeight = Utils.cartesianToGeodetic(this.positionVector, this.spacecraftState.getDate());
    }

    public Orbit getOrbit() {
        return orbit;
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
        return spacecraftState.getDate().toDate(TimeScalesFactory.getUTC());
    }
}
