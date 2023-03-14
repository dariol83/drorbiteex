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

import java.util.Date;

public class TleGenerationRequest {

    private final Orbit orbit;
    private final Date startTime;
    private final int satNumber;
    private final char classification;
    private final int launchYear;
    private final int launchNumber;
    private final String launchPiece;
    private final int revolutionNumberAtEpoch;
    private final Date epoch;
    private final int elementNumber;

    public TleGenerationRequest(Orbit orbit, Date startTime,
                                int satNumber, char classification, int launchYear, int launchNumber, String launchPiece, Date epoch, int revolutionNumberAtEpoch, int elementNumber) {
        this.orbit = orbit;
        this.startTime = startTime;
        this.satNumber = satNumber;
        this.classification = classification;
        this.launchYear = launchYear;
        this.launchNumber = launchNumber;
        this.launchPiece = launchPiece;
        this.epoch = epoch;
        this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
        this.elementNumber = elementNumber;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public Date getStartTime() {
        return startTime;
    }

    public int getSatNumber() {
        return satNumber;
    }

    public char getClassification() {
        return classification;
    }

    public int getLaunchYear() {
        return launchYear;
    }

    public int getLaunchNumber() {
        return launchNumber;
    }

    public String getLaunchPiece() {
        return launchPiece;
    }

    public int getRevolutionNumberAtEpoch() {
        return revolutionNumberAtEpoch;
    }

    public int getElementNumber() {
        return elementNumber;
    }

    public Date getEpoch() {
        return epoch;
    }
}
