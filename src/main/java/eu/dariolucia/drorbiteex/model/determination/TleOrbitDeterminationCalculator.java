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

package eu.dariolucia.drorbiteex.model.determination;

import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class works only with Orbit objects using a TleObjectModel as orbit specification.
 */
public class TleOrbitDeterminationCalculator {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static OrbitDeterminationResult compute(OrbitDeterminationRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        ExecutorService service = Executors.newFixedThreadPool(1, (r) -> {
            Thread t = new Thread(r, "TLE Orbit Determination Task" + request.getOrbit().getName());
            t.setDaemon(true);
            return t;
        });
        // Add the job here
        Future<OrbitDeterminationResult> resultFuture = service.submit(new Worker(request, monitor));
        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        try {
            OrbitDeterminationResult result = resultFuture.get();
            monitor.progress(1, 1, "Done");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            System.gc();
        }
    }

    private static class Worker implements Callable<OrbitDeterminationResult>, BatchLSObserver {
        private final ITaskProgressMonitor monitor;
        private final OrbitDeterminationRequest request;

        public Worker(OrbitDeterminationRequest request, ITaskProgressMonitor monitor) {
            this.monitor = monitor;
            this.request = request;
        }

        @Override
        public OrbitDeterminationResult call() {
            // ---------------------------------------------------
            // Compute required start objects
            // ---------------------------------------------------
            TLE startingTLE = ((TleOrbitModel) request.getOrbit().getModel()).getTleObject();
            AbsoluteDate theStartingTime = retrieveTimeFromMeasures(request.getMeasurementList());
            // ---------------------------------------------------
            // Allocate the estimator
            // ---------------------------------------------------
            TleOrbitDetermination estimator = new TleOrbitDetermination(startingTLE, theStartingTime, request.getMass());
            // Since we use a TLE propagator as input, the propagation will be done using SGP4 propagator, so the initial state can be null
            estimator.initialise(null);
            // ---------------------------------------------------
            // Add the measurements
            // ---------------------------------------------------
            ObservableSatellite os = estimator.getObservableSatellite();
            for(Measurement m : request.getMeasurementList()) {
                estimator.addMeasurements(Collections.singletonList(m.toOrekitMeasurement(os, estimator.getOrbitPropagatorFrame())));
            }
            // ---------------------------------------------------
            // Add the observer
            // ---------------------------------------------------
            estimator.setObserver(this);
            // ---------------------------------------------------
            // Starting the estimation
            // ---------------------------------------------------
            monitor.progress(-1, 0, "Estimating new orbit...");
            TleOrbitDetermination.Result result = estimator.estimate();
            return new OrbitDeterminationResult(request, result.getPropagator(), result.getTle().getLine1() + "\n" + result.getTle().getLine2(), result.getResiduals());
        }

        private AbsoluteDate retrieveTimeFromMeasures(List<Measurement> measurementList) {
            AbsoluteDate startingDate = null;
            for(Measurement measurement : measurementList) {
                AbsoluteDate measTime = measurement.getAbsoluteDate();
                if(startingDate == null || measTime.isBefore(startingDate)) {
                    startingDate = measTime;
                }
            }
            return startingDate;
        }

        @Override
        public void evaluationPerformed(int iterationsCount, int evaluationsCount, Orbit[] orbits, ParameterDriversList estimatedOrbitalParameters, ParameterDriversList estimatedPropagatorParameters, ParameterDriversList estimatedMeasurementsParameters, EstimationsProvider evaluationsProvider, LeastSquaresProblem.Evaluation lspEvaluation) {
            String message = "Iterations: " + iterationsCount + "/" + TleOrbitDetermination.ESTIMATOR_MAX_ITERATIONS +
                    " - Evaluations: " + evaluationsCount + "/" + TleOrbitDetermination.ESTIMATOR_MAX_EVALUATIONS +
                    " - RMS:" + lspEvaluation.getRMS() +
                    " - Nb: " + evaluationsProvider.getNumber();
            monitor.progress(iterationsCount, TleOrbitDetermination.ESTIMATOR_MAX_ITERATIONS, message);
        }
    }
}
