package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.SpacecraftPosition;

import java.util.List;

public interface IOrbitListener {

    void orbitAdded(OrbitManager manager, Orbit orbit);

    void orbitRemoved(OrbitManager manager, Orbit orbit);

    void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition);

    void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition);
}
