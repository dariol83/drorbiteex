package eu.dariolucia.drorbiteex.model;

import org.orekit.propagation.Propagator;

public interface IOrbitModel {

    Propagator getPropagator();

    boolean updateModel(IOrbitModel model);
}
