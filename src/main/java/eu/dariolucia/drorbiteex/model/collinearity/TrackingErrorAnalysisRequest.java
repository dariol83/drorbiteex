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

package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;

import java.util.Date;

public class TrackingErrorAnalysisRequest {

    private final GroundStation groundStation;

    private final Orbit targetOrbit;
    private final Orbit referenceOrbit;
    private final Date startTime;
    private final Date endTime;
    private final int intervalPeriod;

    public TrackingErrorAnalysisRequest(GroundStation groundStation,
                                        Orbit targetOrbit, Orbit referenceOrbit,
                                        Date startTime, Date endTime,
                                        int intervalPeriod) {
        // Copy the ground station and the reference orbit
        this.groundStation = groundStation.copy();
        this.referenceOrbit = referenceOrbit.copy();
        this.targetOrbit = targetOrbit.copy();
        this.startTime = startTime;
        this.endTime = endTime;
        this.intervalPeriod = intervalPeriod;
    }

    public Orbit getTargetOrbit() {
        return targetOrbit;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Orbit getReferenceOrbit() {
        return referenceOrbit;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getIntervalPeriod() {
        return intervalPeriod;
    }

}
