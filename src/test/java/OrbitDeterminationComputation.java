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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.attitudes.NadirPointing;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.data.DirectoryCrawler;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.oem.OemSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.File;
import java.io.FileInputStream;

public class OrbitDeterminationComputation {

    private static final String ORIGINAL_TLE = // Old TLE
            "1 54234U 22150A   23073.00000000  .00000224  00000-0  10631-3 0  1368\n" +
            "2 54234 098.7089 012.4823 0001096 091.3860 344.8096 14.19561677017553";

    private static final String CELESTRAK_TLE = // New TLE - OEM positions derived from here
            "1 54234U 22150A   23082.14800799  .00000237  00000+0  13325-3 0  9999\n" +
            "2 54234  98.7098  21.4778 0001071  76.9929 283.1367 14.19566608 18848";

    public static String determineOrbit(TLE startingTLE, Oem measurements) {
        // Credits: https://nbviewer.org/github/GorgiAstro/laser-orbit-determination/blob/6cafcef83dbc03a61d64417d0aeb0977caf0e064/02-orbit-determination-example.ipynb

        // ---------------------------------------------------
        // Define properties
        // ---------------------------------------------------
        System.out.println("Defining orbit determination properties...");
        // Mass of the S/C in kg
        double mass = 2600;
        // Estimator parameters
        double estimatorPositionScale = 1.0; // m
        double estimatorConvergenceThres = 0.001;
        int estimatorMaxIterations = 25;
        int estimatorMaxEvaluations = 35;
        // ---------------------------------------------------
        // Prepare objects and properties for orbit determination
        // ---------------------------------------------------
        // Frame tod = FramesFactory.getTOD(IERSConventions.IERS_2010, false);// Taking tidal effects into account when interpolating EOP parameters
        Frame gcrf = FramesFactory.getGCRF();
        Frame itrf = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, false);
        // Selecting frames to use for OD
        Frame eci = gcrf;
        Frame ecef = itrf;
        ReferenceEllipsoid wgs84Ellipsoid = ReferenceEllipsoid.getWgs84(ecef);
        // ---------------------------------------------------
        // Setting up propagator
        // ---------------------------------------------------
        System.out.println("Setting up propagator...");
        AbsoluteDate theStartingTime = retrieveTimeFromMeasures(measurements);
        NadirPointing nadirPointing = new NadirPointing(eci, wgs84Ellipsoid);
        SGP4 sgp4Propagator = new SGP4(startingTLE, nadirPointing, mass);
        SpacecraftState initialState = sgp4Propagator.propagate(theStartingTime.shiftedBy(-1)); // Propagate to a date close to the first measurement
        TLEPropagatorBuilder propagatorBuilder = new TLEPropagatorBuilder(TLE.stateToTLE(initialState, startingTLE), PositionAngle.MEAN, estimatorPositionScale);
        propagatorBuilder.setAttitudeProvider(nadirPointing);
        // ---------------------------------------------------
        // Build optimizer and estimator
        // ---------------------------------------------------
        System.out.println("Setting up optimizer and estimator...");
        MatrixDecomposer matrixDecomposer = new QRDecomposer(1e-11);
        GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(matrixDecomposer, false);
        BatchLSEstimator estimator = new BatchLSEstimator(optimizer, propagatorBuilder);
        estimator.setParametersConvergenceThreshold(estimatorConvergenceThres);
        estimator.setMaxIterations(estimatorMaxIterations);
        estimator.setMaxEvaluations(estimatorMaxEvaluations);
        // ---------------------------------------------------
        // Adding the measurements
        // ---------------------------------------------------
        System.out.println("Adding measurements...");
        ObservableSatellite os = new ObservableSatellite(0);
        for(OemSegment segment : measurements.getSegments()) {
            for(TimeStampedPVCoordinates coord : segment.getCoordinates()) {
                PV measurement = mapToMeasurement(coord, segment.getFrame(), propagatorBuilder.getFrame(), os);
                estimator.addMeasurement(measurement);
            }
        }
        // ---------------------------------------------------
        // Starting the estimation
        // ---------------------------------------------------
        System.out.println("Starting estimation...");
        long startTime = System.currentTimeMillis();
        Propagator[] propagators = estimator.estimate();
        System.out.println("Estimation completed in " + (System.currentTimeMillis() - startTime)/1000 + " seconds");
        // Get the final data
        TLEPropagator estimatedPropagator = (TLEPropagator) propagators[0]; // to return
        TLE fitted = estimatedPropagator.getTLE();
        return fitted.getLine1() + "\n" + fitted.getLine2(); // to return
    }

    private static AbsoluteDate retrieveTimeFromMeasures(Oem measurements) {
        AbsoluteDate startingDate = null;
        for(OemSegment segment : measurements.getSegments()) {
            for(TimeStampedPVCoordinates coord : segment.getCoordinates()) {
                AbsoluteDate measTime = coord.getDate();
                if(startingDate == null || measTime.isBefore(startingDate)) {
                    startingDate = measTime;
                }
            }
        }
        return startingDate;
    }

    private static PV mapToMeasurement(TimeStampedPVCoordinates o, Frame measurementFrame, Frame orbitFrame, ObservableSatellite satellite) {
        Vector3D newPosition = measurementFrame.getTransformTo(orbitFrame, o.getDate()).transformPosition(o.getPosition());
        Vector3D newVelocity = measurementFrame.getTransformTo(orbitFrame, o.getDate()).transformVector(o.getVelocity());
        return new PV(o.getDate(), newPosition, newVelocity, 0.2, 0.2, 1.0, satellite);
    }

    public static void main(String[] args) {
        if(args.length != 2) {
            System.err.println("Usage: OrbitDeterminationComputation <path to Orekit data> <path to OEM file>");
            System.exit(-1);
        }
        // Orekit initialisation
        File orekitData = new File(args[0]);
        try {
            DataProvidersManager orekitManager = DataContext.getDefault().getDataProvidersManager();
            orekitManager.addProvider(new DirectoryCrawler(orekitData));
        } catch (Exception e) {
            // You have to quit
            System.err.println("Orekit initialisation data not found");
            e.printStackTrace();
            System.exit(-1);
        }
        // Feed updated information for orbit determination to Orekit. If it fails, keep going
        try {
            MarshallSolarActivityFutureEstimation msafe = new MarshallSolarActivityFutureEstimation(
                    MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                    MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            DataProvidersManager orekitManager = DataContext.getDefault().getDataProvidersManager();
            orekitManager.feed(msafe.getSupportedNames(), msafe); // Feeding the F10.7 bulletins to Orekit's data manager
        } catch (Exception e) {
            System.err.println("Cannot feed F10.7 bulletins to Orekit's data manager");
            e.printStackTrace();
        }
        // Load measurements
        Oem oemObject = new ParserBuilder().buildOemParser().parseMessage(new DataSource("oem", () -> new FileInputStream(args[1])));
        // Build initial TLE
        String[] tleLines = ORIGINAL_TLE.split("\n", -1);
        TLE initialTle = new TLE(tleLines[0], tleLines[1]);
        // Determine new TLE
        String newTle = determineOrbit(initialTle, oemObject);
        // Print new TLE
        System.out.println(newTle);
    }

}
