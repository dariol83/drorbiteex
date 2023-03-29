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
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.AbstractMeasurement;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TleOrbitDetermination {

    private final static double DEFAULT_MASS = 1000; // in Kg

    // Input data
    private final TLE startingTLE;
    private final AbsoluteDate startingTime;
    private final double mass;

    // Estimator parameters
    public static final double ESTIMATOR_POSITION_SCALE = 0.1; // m
    public static final double ESTIMATOR_CONVERGENCE_THRES = 0.0001;
    public static final int ESTIMATOR_MAX_ITERATIONS = 25;
    public static final int ESTIMATOR_MAX_EVALUATIONS = 35;
    private BatchLSObserver observer;
    private Frame orbitPropagatorFrame;
    private BatchLSEstimator estimator;
    private final List<AbstractMeasurement<?>> measurements = new LinkedList<>();
    private ObservableSatellite observableSatellite;

    private boolean initialised = false;

    public TleOrbitDetermination(TLE startingTLE, AbsoluteDate startingTime) {
        this(startingTLE, startingTime, DEFAULT_MASS);
    }

    public TleOrbitDetermination(TLE startingTLE, AbsoluteDate startingTime, double mass) {
        this.startingTLE = startingTLE;
        this.startingTime = startingTime;
        this.mass = mass;
    }

    public void initialise(SpacecraftState initialState) {
        if(initialised) {
            throw new IllegalStateException("Already initialised");
        }
        // Credits: https://nbviewer.org/github/GorgiAstro/laser-orbit-determination/blob/6cafcef83dbc03a61d64417d0aeb0977caf0e064/02-orbit-determination-example.ipynb
        // Credits: https://gitlab.orekit.org/orekit/orekit-tutorials/-/blob/master/src/main/java/org/orekit/tutorials/estimation/TLEBasedOrbitDetermination.java
        // Credits: https://forum.orekit.org/t/issue-with-orbit-determination-from-oem-measurements/2410/3

        // Prepare objects and properties for orbit determination
        Frame gcrf = FramesFactory.getGCRF();
        Frame itrf = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, false);
        // Selecting frames to use for OD
        ReferenceEllipsoid wgs84Ellipsoid = ReferenceEllipsoid.getWgs84(itrf);
        // ---------------------------------------------------
        // Prepare the initial state
        // ---------------------------------------------------
        NadirPointing nadirPointing = new NadirPointing(gcrf, wgs84Ellipsoid);
        if(initialState == null) {
            SGP4 sgp4Propagator = new SGP4(startingTLE, nadirPointing, mass);
            initialState = sgp4Propagator.propagate(startingTime.shiftedBy(-1)); // Propagate to a date close to the first measurement
        }
        // ---------------------------------------------------
        // Setting up propagator
        // ---------------------------------------------------
        TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(TLE.stateToTLE(initialState, startingTLE), PositionAngle.MEAN, ESTIMATOR_POSITION_SCALE);
        propagatorBuilder.setAttitudeProvider(nadirPointing);
        this.orbitPropagatorFrame = propagatorBuilder.getFrame();
        // ---------------------------------------------------
        // Setting up the estimator
        // ---------------------------------------------------
        MatrixDecomposer matrixDecomposer = new QRDecomposer(1e-11);
        GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(matrixDecomposer, false);
        this.estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
        this.estimator.setParametersConvergenceThreshold(ESTIMATOR_CONVERGENCE_THRES);
        this.estimator.setMaxIterations(ESTIMATOR_MAX_ITERATIONS);
        this.estimator.setMaxEvaluations(ESTIMATOR_MAX_EVALUATIONS);
        // The satellite
        this.observableSatellite = new ObservableSatellite(0);

        this.initialised = true;
    }

    public Result estimate() {
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
        TLEPropagator estimatedPropagator = (TLEPropagator) propagators[0]; // to return
        // Generate TLE
        TLE fitted = estimatedPropagator.getTLE();
        // Residuals
        Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> lastEstimations = estimator.getLastEstimations();
        List<ErrorPoint> residuals = new ArrayList<>(); // to return
        for (Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> e : lastEstimations.entrySet()) {
            double[] estimated = e.getValue().getEstimatedValue();
            double[] observed = e.getValue().getObservedValue();
            AbsoluteDate time = e.getValue().getDate();
            residuals.add(new ErrorPoint(TimeUtils.toInstant(time), estimated[0], observed[0], observed[0] - estimated[0]));
        }
        return new Result(fitted, estimatedPropagator, residuals);
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
        private final TLE tle;
        private final TLEPropagator propagator;
        private final List<ErrorPoint> residuals;

        public Result(TLE tle, TLEPropagator propagator, List<ErrorPoint> residuals) {
            this.tle = tle;
            this.propagator = propagator;
            this.residuals = residuals;
        }

        public TLE getTle() {
            return tle;
        }

        public TLEPropagator getPropagator() {
            return propagator;
        }

        public List<ErrorPoint> getResiduals() {
            return residuals;
        }
    }
}
