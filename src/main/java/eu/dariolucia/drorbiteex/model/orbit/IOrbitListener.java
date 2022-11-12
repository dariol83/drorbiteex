package eu.dariolucia.drorbiteex.model.orbit;

import java.util.Date;
import java.util.List;

public interface IOrbitListener {

    void orbitAdded(OrbitManager manager, Orbit orbit);

    void orbitRemoved(OrbitManager manager, Orbit orbit);

    void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition);

    void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition);

    default void startOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        // Do nothing
    }

    default void endOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        // Do nothing
    }
}
