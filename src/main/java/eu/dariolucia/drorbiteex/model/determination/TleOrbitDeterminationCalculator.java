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

import eu.dariolucia.drorbiteex.model.collinearity.ErrorPoint;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static class Worker implements Callable<OrbitDeterminationResult> {
        private final ITaskProgressMonitor monitor;
        private final OrbitDeterminationRequest request;

        public Worker(OrbitDeterminationRequest request, ITaskProgressMonitor monitor) {
            this.monitor = monitor;
            this.request = request;
        }

        @Override
        public OrbitDeterminationResult call() {
            // Estimator parameters
            double estimatorPositionScale = 1.0; // m
            double estimatorConvergenceThres = 0.001;
            int estimatorMaxIterations = 25;
            int estimatorMaxEvaluations = 35;

            // Credits: https://nbviewer.org/github/GorgiAstro/laser-orbit-determination/blob/6cafcef83dbc03a61d64417d0aeb0977caf0e064/02-orbit-determination-example.ipynb
            // Credits: https://gitlab.orekit.org/orekit/orekit-tutorials/-/blob/master/src/main/java/org/orekit/tutorials/estimation/TLEBasedOrbitDetermination.java
            // Credits: https://forum.orekit.org/t/issue-with-orbit-determination-from-oem-measurements/2410/3

            // Prepare objects and properties for orbit determination
            Frame gcrf = FramesFactory.getGCRF();
            Frame itrf = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, false);
            // Selecting frames to use for OD
            ReferenceEllipsoid wgs84Ellipsoid = ReferenceEllipsoid.getWgs84(itrf);
            // ---------------------------------------------------
            // Setting up propagator
            // ---------------------------------------------------
            TLE startingTLE = ((TleOrbitModel) request.getOrbit().getModel()).getTleObject();
            AbsoluteDate theStartingTime = retrieveTimeFromMeasures(request.getMeasurementList());
            NadirPointing nadirPointing = new NadirPointing(gcrf, wgs84Ellipsoid);
            SGP4 sgp4Propagator = new SGP4(startingTLE, nadirPointing, request.getMass());
            SpacecraftState initialState = sgp4Propagator.propagate(theStartingTime.shiftedBy(-1)); // Propagate to a date close to the first measurement
            TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(TLE.stateToTLE(initialState, startingTLE), PositionAngle.MEAN, estimatorPositionScale);
            propagatorBuilder.setAttitudeProvider(nadirPointing);
            // ---------------------------------------------------
            // Setting up the estimator
            // ---------------------------------------------------
            MatrixDecomposer matrixDecomposer = new QRDecomposer(1e-11);
            GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(matrixDecomposer, false);
            BatchLSEstimator estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
            estimator.setParametersConvergenceThreshold(estimatorConvergenceThres);
            estimator.setMaxIterations(estimatorMaxIterations);
            estimator.setMaxEvaluations(estimatorMaxEvaluations);
            // ---------------------------------------------------
            // Adding the measurements
            // ---------------------------------------------------
            ObservableSatellite os = new ObservableSatellite(0);
            for(Measurement m : request.getMeasurementList()) {
                estimator.addMeasurement(m.toOrekitMeasurement(os, propagatorBuilder.getFrame()));
            }
            // ---------------------------------------------------
            // Starting the estimation
            // ---------------------------------------------------
            monitor.progress(-1, 0, "Estimating new orbit...");
            Propagator[] propagators = estimator.estimate();
            // Get the final data
            TLEPropagator estimatedPropagator = (TLEPropagator) propagators[0]; // to return
            // Generate TLE
            TLE fitted = estimatedPropagator.getTLE();
            String tleLines = fitted.getLine1() + "\n" + fitted.getLine2(); // to return
            System.out.println(tleLines);
            // Residuals
            Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> lastEstimations = estimator.getLastEstimations();
            List<ErrorPoint> residuals = new ArrayList<>(); // to return
            for(Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> e : lastEstimations.entrySet()) {
                double[] estimated = e.getValue().getEstimatedValue();
                double[] observed = e.getValue().getObservedValue();
                AbsoluteDate time = e.getValue().getDate();
                residuals.add(new ErrorPoint(TimeUtils.toInstant(time), estimated[0], observed[0], observed[0] - estimated[0]));
            }
            return new OrbitDeterminationResult(request, estimatedPropagator, tleLines, residuals);
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
    }
}
