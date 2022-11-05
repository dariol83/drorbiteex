package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.SpacecraftPosition;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.EventDetector;

import java.util.Date;

public interface IOrbitVisibilityProcessor extends IOrbitListener {

    EventDetector getEventDetector();

    void startVisibilityComputation(Orbit o);

    void initVisibilityComputation(Orbit o, Date startTime);

    void finalizeVisibilityComputation(Orbit o, SpacecraftPosition currentSpacecraftPosition);

    void propagationModelAvailable(Orbit orbit, Date referenceDate, Propagator modelPropagator);

    void endVisibilityComputation(Orbit o);
}
