package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import org.orekit.bodies.GeodeticPoint;

import java.util.List;

public interface IGroundStationListener {
    void groundStationAdded(GroundStationManager manager, GroundStation groundStation);

    void groundStationRemoved(GroundStationManager manager, GroundStation groundStation);

    void groundStationUpdated(GroundStation groundStation);

    void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint);

    void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point);

}
