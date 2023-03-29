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

package eu.dariolucia.drorbiteex.model.tle;

import eu.dariolucia.drorbiteex.model.determination.Measurement;
import eu.dariolucia.drorbiteex.model.determination.TleOrbitDetermination;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

import java.util.Collections;

public class TleExporterProcess {
    public String exportTle(TleGenerationRequest request, ITaskProgressMonitor monitor) {
        // ---------------------------------------------------
        // Compute required start objects
        // ---------------------------------------------------
        Orbit toPropagate = request.getOrbit().copy();
        AbsoluteDate startTime = TimeUtils.toAbsoluteDate(request.getStartTime());
        TLE startingTLE = TleUtils.getTleFrom(request, toPropagate);
        // ---------------------------------------------------
        // Allocate the estimator
        // ---------------------------------------------------
        TleOrbitDetermination estimator = new TleOrbitDetermination(startingTLE, startTime); // Use default mass
        // ---------------------------------------------------
        // Compute the initial state
        // ---------------------------------------------------
        Propagator p = toPropagate.getModel().getPropagator();
        SpacecraftState initialState = p.propagate(startTime);
        estimator.initialise(initialState);
        // ---------------------------------------------------
        // Add the measurements - Propagate and compute values
        // ---------------------------------------------------
        ObservableSatellite os = estimator.getObservableSatellite();
        Frame orbitFrame = estimator.getOrbitPropagatorFrame();
        // Propagate the propagator until you can (7 days max), step 60 seconds
        AbsoluteDate currentTime = startTime;
        AbsoluteDate endTime = startTime.shiftedBy(3600 * 24 * 7.0);
        while(currentTime.isBefore(endTime)) {
            try {
                SpacecraftState position = p.propagate(currentTime);
                Vector3D newPosition = position.getPVCoordinates(orbitFrame).getPosition();
                Vector3D newVelocity = position.getPVCoordinates(orbitFrame).getVelocity();
                PV measurement = new PV(position.getDate(),newPosition, newVelocity,
                        0.1, 0.1, 1.0, os);
                estimator.addMeasurements(Collections.singletonList(measurement));
                currentTime = currentTime.shiftedBy(60);
            } catch (Exception e) {
                // Stop propagation
            }
        }
        // ---------------------------------------------------
        // Add the observer
        // ---------------------------------------------------
        estimator.setObserver(buildTaskMonitorWrapper(monitor));
        // ---------------------------------------------------
        // Starting the estimation
        // ---------------------------------------------------
        monitor.progress(-1, 0, "Estimating new orbit...");
        TleOrbitDetermination.Result result = estimator.estimate();
        return result.getTle().getLine1() + "\n" + result.getTle().getLine2();
    }

    private BatchLSObserver buildTaskMonitorWrapper(ITaskProgressMonitor monitor) {
        return (iterationsCount, evaluationsCount, orbits, estimatedOrbitalParameters, estimatedPropagatorParameters, estimatedMeasurementsParameters, evaluationsProvider, lspEvaluation) -> {
            String message = "Iterations: " + iterationsCount + "/" + TleOrbitDetermination.ESTIMATOR_MAX_ITERATIONS +
                    " - Evaluations: " + evaluationsCount + "/" + TleOrbitDetermination.ESTIMATOR_MAX_EVALUATIONS +
                    " - RMS:" + lspEvaluation.getRMS() +
                    " - Nb: " + evaluationsProvider.getNumber();
            monitor.progress(iterationsCount, TleOrbitDetermination.ESTIMATOR_MAX_ITERATIONS, message);
        };
    }

}
