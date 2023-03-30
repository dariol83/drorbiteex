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
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.tle.TleGenerationRequest;
import eu.dariolucia.drorbiteex.model.tle.TleUtils;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class works with all Orbit objects.
 */
public class OrbitDeterminationCalculator {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static NumericalOrbitDeterminationResult compute(NumericalOrbitDeterminationRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        ExecutorService service = Executors.newFixedThreadPool(1, (r) -> {
            Thread t = new Thread(r, "Orbit Determination Task" + request.getOrbit().getName());
            t.setDaemon(true);
            return t;
        });
        // Add the job here
        Future<NumericalOrbitDeterminationResult> resultFuture = service.submit(new Worker(request, monitor));
        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        try {
            NumericalOrbitDeterminationResult result = resultFuture.get();
            monitor.progress(1, 1, "Done");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            System.gc();
        }
    }

    private static class Worker implements Callable<NumericalOrbitDeterminationResult> {
        private final ITaskProgressMonitor monitor;
        private final NumericalOrbitDeterminationRequest request;

        public Worker(NumericalOrbitDeterminationRequest request, ITaskProgressMonitor monitor) {
            this.monitor = monitor;
            this.request = request;
        }

        @Override
        public NumericalOrbitDeterminationResult call() {
            // Orbit propagator parameters
            double propMinStep = 0.001; // s
            double propMaxStep = 300.0; // s
            double propPositionError = 1.0; // m

            // Estimator parameters
            double estimatorPositionScale = 1.0; // m
            double estimatorConvergenceThres = 0.001;
            int estimatorMaxIterations = 25;
            int estimatorMaxEvaluations = 35;

            Orbit startingOrbit = request.getOrbit().copy();
            // Credits: https://nbviewer.org/github/GorgiAstro/laser-orbit-determination/blob/6cafcef83dbc03a61d64417d0aeb0977caf0e064/02-orbit-determination-example.ipynb
            // Prepare objects and properties for orbit determination
            Frame gcrf = FramesFactory.getGCRF();
            Frame itrf = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, false);
            // Selecting frames to use for OD
            Frame eci = gcrf;
            Frame ecef = itrf;
            ReferenceEllipsoid wgs84Ellipsoid = ReferenceEllipsoid.getWgs84(ecef);

            // TimeScale utc = TimeScalesFactory.getUTC();
            // Setting up propagator - if TLE, convert to CartesianOrbit
            NadirPointing nadirPointing = new NadirPointing(eci, wgs84Ellipsoid);
            Propagator propagator = startingOrbit.getModel().getPropagator();
            SpacecraftState initialState = propagator.getInitialState();
            // AbsoluteDate epoch = initialState.getDate();
            org.orekit.orbits.Orbit originalOrbit = initialState.getOrbit();
            TimeStampedPVCoordinates pvEci = originalOrbit.getPVCoordinates(eci);
            org.orekit.orbits.Orbit eciOrbit = new CartesianOrbit(pvEci, eci, wgs84Ellipsoid.getGM());
            DormandPrince853IntegratorBuilder integratorBuilder = new DormandPrince853IntegratorBuilder(propMinStep, propMaxStep, propPositionError);
            NumericalPropagatorBuilder propagatorBuilder = new NumericalPropagatorBuilder(eciOrbit, integratorBuilder, PositionAngle.MEAN, estimatorPositionScale);
            propagatorBuilder.setMass(request.getMass());
            propagatorBuilder.setAttitudeProvider(nadirPointing);
            // Add perturbation forces to the propagator
            addPerturbationForces(ecef, wgs84Ellipsoid, propagatorBuilder);
            // Setting up the estimator
            MatrixDecomposer matrixDecomposer = new QRDecomposer(1e-11);
            GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(matrixDecomposer, false);
            BatchLSEstimator estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
            estimator.setParametersConvergenceThreshold(estimatorConvergenceThres);
            estimator.setMaxIterations(estimatorMaxIterations);
            estimator.setMaxEvaluations(estimatorMaxEvaluations);
            // Adding the measurements
            ObservableSatellite os = new ObservableSatellite(0);
            for(Measurement m : request.getMeasurementList()) {
                estimator.addMeasurement(m.toOrekitMeasurement(os, eciOrbit.getFrame()));
            }
            // Starting the estimation
            monitor.progress(-1, 0, "Estimating new orbit...");
            Propagator[] propagators = estimator.estimate();
            // Get the final data
            Propagator estimatedPropagator = propagators[0]; // to return
            SpacecraftState estimatedInitialState = estimatedPropagator.getInitialState();
            // Generate TLE
            TLE initialTle = TleUtils.getTleFrom(TleGenerationRequest.fromOrbit(startingOrbit), startingOrbit);
            TLE fitted = TLE.stateToTLE(estimatedInitialState, initialTle);
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
            // return new TleOrbitDeterminationResult(request, estimatedPropagator, tleLines, residuals);
            return null; // TODO: export OEM
        }

        private void addPerturbationForces(Frame ecef, ReferenceEllipsoid wgs84Ellipsoid, NumericalPropagatorBuilder propagatorBuilder) {
            // Earth gravity field with degree 64 and order 64
            NormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getConstantNormalizedProvider(64, 64);
            HolmesFeatherstoneAttractionModel gravityAttractionModel = new HolmesFeatherstoneAttractionModel(ecef, gravityProvider);
            propagatorBuilder.addForceModel(gravityAttractionModel);
            // Moon and Sun perturbations
            CelestialBody sun = CelestialBodyFactory.getSun();
            CelestialBody moon = CelestialBodyFactory.getMoon();
            if(request.isUseMoon()) {
                propagatorBuilder.addForceModel(new ThirdBodyAttraction(moon));
            }
            if(request.isUseSun()) {
                propagatorBuilder.addForceModel(new ThirdBodyAttraction(sun));
            }
            // Solar radiation pressure
            if(request.isUseSolarPressure()) {
                IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoeff = new IsotropicRadiationSingleCoefficient(request.getCrossSection(), request.getCr());
                SolarRadiationPressure solarRadiationPressure = new SolarRadiationPressure(sun, wgs84Ellipsoid.getEquatorialRadius(), isotropicRadiationSingleCoeff);
                propagatorBuilder.addForceModel(solarRadiationPressure);
            }
            // Atmospheric drag
            if(request.isUseAtmosphericDrag()) {
                MarshallSolarActivityFutureEstimation msafe = new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES, MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
                NRLMSISE00 atmosphere = new NRLMSISE00(msafe, sun, wgs84Ellipsoid);
                // atmosphere = DTM2000(msafe, sun, wgs84Ellipsoid);
                IsotropicDrag isotropicDrag = new IsotropicDrag(request.getCrossSection(), request.getCd());
                DragForce dragForce = new DragForce(atmosphere, isotropicDrag);
                propagatorBuilder.addForceModel(dragForce);
            }
            // Relativity
            if(request.isUseRelativity()) {
                propagatorBuilder.addForceModel(new Relativity(Constants.EIGEN5C_EARTH_MU));
            }
        }
    }
}
