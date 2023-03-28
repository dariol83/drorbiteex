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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

import java.util.Date;

public class TleUtils {

    public static String exportTle(TleGenerationRequest tleGenerationRequest) {
        /*
        // TODO: refactor code so that it is in one place
        // First, copy the orbit
        Orbit toPropagate = tleGenerationRequest.getOrbit().copy();
        AbsoluteDate startTime = TimeUtils.toAbsoluteDate(tleGenerationRequest.getStartTime());
        Propagator p = toPropagate.getModel().getPropagator();

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
        TLE startingTLE = TleUtils.getTleFrom(tleGenerationRequest, toPropagate);
        NadirPointing nadirPointing = new NadirPointing(gcrf, wgs84Ellipsoid);
        SpacecraftState initialState = p.propagate(startTime);
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
        // Propagate the propagator until you can (7 days max), step 10 seconds
        AbsoluteDate currentTime = startTime;
        AbsoluteDate endTime = startTime.shiftedBy(3600 * 24 * 7.0);
        while(currentTime.isBefore(endTime)) {
            try {
                SpacecraftState position = p.propagate(currentTime);
                Vector3D newPosition = position.getPVCoordinates(propagatorBuilder.getFrame()).getPosition();
                Vector3D newVelocity = position.getPVCoordinates(propagatorBuilder.getFrame()).getVelocity();
                PV measurement = new PV(position.getDate(),newPosition, newVelocity,
                        0.2, 0.2, 1.0, os);
                estimator.addMeasurement(measurement);
                currentTime = currentTime.shiftedBy(10);
            } catch (Exception e) {
                // Stop propagation
            }
        }
        // ---------------------------------------------------
        // Starting the estimation
        // ---------------------------------------------------
        Propagator[] propagators = estimator.estimate();
        // TODO: add estimator observer
        // Get the final data
        TLEPropagator estimatedPropagator = (TLEPropagator) propagators[0]; // to return
        // Generate TLE
        TLE fitted = estimatedPropagator.getTLE();
        return fitted.getLine1() + "\n" + fitted.getLine2();
        */

        // Old code: inaccurate as hell
        // First, copy the orbit
        Orbit toPropagate = tleGenerationRequest.getOrbit().copy();
        AbsoluteDate startTime = TimeUtils.toAbsoluteDate(tleGenerationRequest.getStartTime());
        Propagator p = toPropagate.getModel().getPropagator();
        SpacecraftState firstState = p.propagate(startTime);

        // Let's go for the TLE (https://forum.orekit.org/t/generation-of-tle/265/4)
        // You need an initial TLE for the job... so let's build one
        TLE initialTle = TleUtils.getTleFrom(tleGenerationRequest, toPropagate);
        // Now derive the TLE
        TLE fitted = TLE.stateToTLE(firstState, initialTle);
        return fitted.getLine1() + "\n" + fitted.getLine2();
    }

    public static TLE getTleFrom(TleGenerationRequest tleGenerationRequest, Orbit toPropagate) {
        TLE initialTle = null;
        if(toPropagate.getModel() instanceof TleOrbitModel) {
            // Original orbit is a TLE
            initialTle = ((TleOrbitModel) toPropagate.getModel()).getTleObject();
        } else {
            // Original orbit is not a TLE
            initialTle = TleUtils.initialiseTleFromPropagator(toPropagate.getModel().getPropagator(), tleGenerationRequest);
        }
        return initialTle;
    }

    public static TLE computeEmptyTleFrom(Propagator propagator) {
        return computeTleFrom(propagator,
                0,'U',2000, 1,"A", 0, propagator.getInitialState().getDate(), 999);
    }

    public static TLE computeTleFrom(Propagator propagator, int satNumber, char classification, int launchYear, int launchNumber, String launchPiece, int revolutionNumberAtEpoch, AbsoluteDate epoch, int elementNumber) {
        // Set meanMotionFirstDerivative, meanMotionSecondDerivative, bStar to zero
        double meanMotionFirstDerivative = 0;
        double meanMotionSecondDerivative = 0;
        double bStar = 0;

        // Get the inner orbit and convert to Keplerian orbit if needed
        KeplerianOrbit keplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagator.getInitialState().getOrbit());
        double e = keplerianOrbit.getE();
        double i = keplerianOrbit.getI();
        double meanMotion = keplerianOrbit.getKeplerianMeanMotion();
        int ephemerisType = TLE.DEFAULT;

        double pa = keplerianOrbit.getPerigeeArgument();
        double raan = keplerianOrbit.getRightAscensionOfAscendingNode();
        double meanAnomaly = keplerianOrbit.getMeanAnomaly();

        return new TLE(satNumber, classification, launchYear, launchNumber, launchPiece, ephemerisType, elementNumber,
                epoch, meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative, e, i, pa, raan, meanAnomaly, revolutionNumberAtEpoch,
                bStar);
    }

    public static TLE initialiseTleFromPropagator(Propagator propagator, TleGenerationRequest request) {
        int satNumber = request.getSatNumber();
        char classification = request.getClassification();
        int launchYear = request.getLaunchYear();
        int launchNumber = request.getLaunchNumber();
        String launchPiece = request.getLaunchPiece();
        int revolutionNumberAtEpoch = request.getRevolutionNumberAtEpoch();
        Date epochDate = request.getEpoch();
        AbsoluteDate epoch = TimeUtils.toAbsoluteDate(epochDate);
        int elementNumber = request.getElementNumber();

        return TleUtils.computeTleFrom(propagator, satNumber, classification, launchYear, launchNumber, launchPiece, revolutionNumberAtEpoch, epoch, elementNumber);
    }
}
