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
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.AbstractMeasurement;
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
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class works with all Orbit objects.
 */
public class NumericalOrbitDetermination {

    // Input data
    private final Orbit startingOrbit;
    private final AbsoluteDate startingTime;
    private final double mass; // kg

    // Estimator parameters
    public static final double ESTIMATOR_POSITION_SCALE = 1.0; // m
    public static final double ESTIMATOR_CONVERGENCE_THRES = 0.01;
    public static final int ESTIMATOR_MAX_ITERATIONS = 25;
    public static final int ESTIMATOR_MAX_EVALUATIONS = 35;
    private BatchLSObserver observer;
    private Frame orbitPropagatorFrame;
    private BatchLSEstimator estimator;
    private final List<AbstractMeasurement<?>> measurements = new LinkedList<>();
    private ObservableSatellite observableSatellite;

    private boolean initialised = false;
    private final boolean useMoon;
    private final boolean useSun;
    private final boolean useSolarPressure;
    private final boolean useAtmosphericDrag;
    private final boolean useRelativity;
    private final double crossSection; // mq
    private final double cr;
    private final double cd;

    public NumericalOrbitDetermination(Orbit startingOrbit, AbsoluteDate startingTime, double mass, boolean useMoon, boolean useSun, boolean useSolarPressure, boolean useAtmosphericDrag, boolean useRelativity, double crossSection, double cr, double cd) {
        this.startingOrbit = startingOrbit;
        this.startingTime = startingTime;
        this.mass = mass;
        this.useMoon = useMoon;
        this.useSun = useSun;
        this.useSolarPressure = useSolarPressure;
        this.useAtmosphericDrag = useAtmosphericDrag;
        this.useRelativity = useRelativity;
        this.crossSection = crossSection;
        this.cr = cr;
        this.cd = cd;
    }

    public void initialise(SpacecraftState initialState) {
        if(initialised) {
            throw new IllegalStateException("Already initialised");
        }
        // Credits: https://nbviewer.org/github/GorgiAstro/laser-orbit-determination/blob/6cafcef83dbc03a61d64417d0aeb0977caf0e064/02-orbit-determination-example.ipynb
        // Credits: https://gitlab.orekit.org/orekit/orekit-tutorials/-/blob/master/src/main/java/org/orekit/tutorials/estimation/TLEBasedOrbitDetermination.java
        // Credits: https://forum.orekit.org/t/issue-with-orbit-determination-from-oem-measurements/2410/3

        // Orbit propagator parameters
        double propMinStep = 0.001; // s
        double propMaxStep = 300.0; // s
        double propPositionError = 1.0; // m

        // Prepare objects and properties for orbit determination
        Frame gcrf = FramesFactory.getGCRF();
        Frame itrf = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, false);
        Frame eci = gcrf;
        Frame ecef = itrf;
        // Selecting frames to use for OD
        ReferenceEllipsoid wgs84Ellipsoid = ReferenceEllipsoid.getWgs84(itrf);
        // ---------------------------------------------------
        // Prepare the initial state
        // ---------------------------------------------------
        NadirPointing nadirPointing = new NadirPointing(gcrf, wgs84Ellipsoid);
        if(initialState == null) {
            Propagator propagator = startingOrbit.getModel().getPropagator();
            initialState = propagator.propagate(startingTime.shiftedBy(-1)); // Propagate to a date close to the first measurement
        }
        // ---------------------------------------------------
        // Setting up propagator
        // ---------------------------------------------------
        org.orekit.orbits.Orbit originalOrbit = initialState.getOrbit();
        TimeStampedPVCoordinates pvEci = originalOrbit.getPVCoordinates(eci);
        org.orekit.orbits.Orbit eciOrbit = new CartesianOrbit(pvEci, eci, wgs84Ellipsoid.getGM());
        DormandPrince853IntegratorBuilder integratorBuilder = new DormandPrince853IntegratorBuilder(propMinStep, propMaxStep, propPositionError);
        NumericalPropagatorBuilder propagatorBuilder = new NumericalPropagatorBuilder(eciOrbit, integratorBuilder, PositionAngle.MEAN, ESTIMATOR_POSITION_SCALE);
        propagatorBuilder.setMass(this.mass);
        propagatorBuilder.setAttitudeProvider(nadirPointing);
        this.orbitPropagatorFrame = propagatorBuilder.getFrame();
        // ---------------------------------------------------
        // Setting up the estimator
        // ---------------------------------------------------
        // Add perturbation forces to the propagator
        addPerturbationForces(ecef, wgs84Ellipsoid, propagatorBuilder);
        // Setting up the estimator
        MatrixDecomposer matrixDecomposer = new QRDecomposer(1e-11);
        GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(matrixDecomposer, false);
        this.estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
        estimator.setParametersConvergenceThreshold(ESTIMATOR_CONVERGENCE_THRES);
        estimator.setMaxIterations(ESTIMATOR_MAX_ITERATIONS);
        estimator.setMaxEvaluations(ESTIMATOR_MAX_EVALUATIONS);
        // The satellite
        this.observableSatellite = new ObservableSatellite(0);

        this.initialised = true;
    }

    private void addPerturbationForces(Frame ecef, ReferenceEllipsoid wgs84Ellipsoid, NumericalPropagatorBuilder propagatorBuilder) {
        // Earth gravity field with degree 64 and order 64
        NormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getConstantNormalizedProvider(64, 64);
        HolmesFeatherstoneAttractionModel gravityAttractionModel = new HolmesFeatherstoneAttractionModel(ecef, gravityProvider);
        propagatorBuilder.addForceModel(gravityAttractionModel);
        // Moon and Sun perturbations
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        if(this.useMoon) {
            propagatorBuilder.addForceModel(new ThirdBodyAttraction(moon));
        }
        if(this.useSun) {
            propagatorBuilder.addForceModel(new ThirdBodyAttraction(sun));
        }
        // Solar radiation pressure
        if(this.useSolarPressure) {
            IsotropicRadiationSingleCoefficient isotropicRadiationSingleCoeff = new IsotropicRadiationSingleCoefficient(this.crossSection, this.cr);
            SolarRadiationPressure solarRadiationPressure = new SolarRadiationPressure(sun, wgs84Ellipsoid.getEquatorialRadius(), isotropicRadiationSingleCoeff);
            propagatorBuilder.addForceModel(solarRadiationPressure);
        }
        // Atmospheric drag
        if(this.useAtmosphericDrag) {
            MarshallSolarActivityFutureEstimation msafe = new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES, MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            NRLMSISE00 atmosphere = new NRLMSISE00(msafe, sun, wgs84Ellipsoid);
            // atmosphere = DTM2000(msafe, sun, wgs84Ellipsoid);
            IsotropicDrag isotropicDrag = new IsotropicDrag(this.crossSection, this.cd);
            DragForce dragForce = new DragForce(atmosphere, isotropicDrag);
            propagatorBuilder.addForceModel(dragForce);
        }
        // Relativity
        if(this.useRelativity) {
            propagatorBuilder.addForceModel(new Relativity(Constants.EIGEN5C_EARTH_MU));
        }
    }

    public NumericalOrbitDetermination.Result estimate() {
        if(!initialised) {
            throw new IllegalStateException("Not initialised");
        }
        // ---------------------------------------------------
        // Adding the measurements
        // ---------------------------------------------------
        for (AbstractMeasurement<?> m : this.measurements) {
            estimator.addMeasurement(m);
        }
        // ---------------------------------------------------
        // Adding the observers
        // ---------------------------------------------------
        estimator.setObserver(this.observer);
        // ---------------------------------------------------
        // Starting the estimation
        // ---------------------------------------------------
        Propagator[] propagators = estimator.estimate();
        // Get the final data
        Propagator estimatedPropagator = propagators[0]; // to return
        // Generate TLE
        org.orekit.orbits.Orbit finalOrbit = estimatedPropagator.getInitialState().getOrbit();
        // Residuals
        Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> lastEstimations = estimator.getLastEstimations();
        List<ErrorPoint> residuals = new ArrayList<>(); // to return
        for (Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> e : lastEstimations.entrySet()) {
            double[] estimated = e.getValue().getEstimatedValue();
            double[] observed = e.getValue().getObservedValue();
            AbsoluteDate time = e.getValue().getDate();
            residuals.add(new ErrorPoint(TimeUtils.toInstant(time), estimated[0], observed[0], observed[0] - estimated[0]));
        }
        return new NumericalOrbitDetermination.Result(finalOrbit, estimatedPropagator, residuals);
    }

    public Frame getOrbitPropagatorFrame() {
        if(!initialised) {
            throw new IllegalStateException("Not initialised");
        }
        return orbitPropagatorFrame;
    }

    public ObservableSatellite getObservableSatellite() {
        if(!initialised) {
            throw new IllegalStateException("Not initialised");
        }
        return observableSatellite;
    }

    public void setObserver(BatchLSObserver observer) {
        if(!initialised) {
            throw new IllegalStateException("Not initialised");
        }
        this.observer = observer;
    }

    public void addMeasurements(List<AbstractMeasurement<?>> measurements) {
        if(!initialised) {
            throw new IllegalStateException("Not initialised");
        }
        this.measurements.addAll(measurements);
    }

    public static class Result {
        private final org.orekit.orbits.Orbit finalOrbit;
        private final Propagator propagator;
        private final List<ErrorPoint> residuals;

        public Result(org.orekit.orbits.Orbit finalOrbit, Propagator propagator, List<ErrorPoint> residuals) {
            this.finalOrbit = finalOrbit;
            this.propagator = propagator;
            this.residuals = residuals;
        }

        public org.orekit.orbits.Orbit getFinalOrbit() {
            return finalOrbit;
        }

        public Propagator getPropagator() {
            return propagator;
        }

        public List<ErrorPoint> getResiduals() {
            return residuals;
        }
    }
}
