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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;

import java.util.Date;

public class TleUtils {

    // Deprecated: TLE fit as per https://forum.orekit.org/t/generation-of-tle/265/4

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
