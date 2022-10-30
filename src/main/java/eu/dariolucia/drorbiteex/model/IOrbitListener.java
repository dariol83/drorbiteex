package eu.dariolucia.drorbiteex.model;

import org.orekit.propagation.events.EventDetector;

import java.util.List;

public interface IOrbitListener {

    IVisibilityDetector getEventDetector();

    void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition);

    void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition);
}
