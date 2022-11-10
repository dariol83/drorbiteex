package eu.dariolucia.drorbiteex.model.orbit;

import org.orekit.propagation.Propagator;

import java.util.Date;

public interface IOrbitModel {

    Propagator getPropagator();

    boolean updateModel(IOrbitModel model);

    int computeOrbitNumberAt(Date time);

    IOrbitModel copy();
}
