package eu.dariolucia.drorbiteex.model;

import org.orekit.propagation.events.EventDetector;

import java.util.Date;

public interface IVisibilityDetector extends EventDetector {

    void initVisibilityComputation(Orbit o, Date startTime);

    void endVisibilityComputation(Orbit o);
}
