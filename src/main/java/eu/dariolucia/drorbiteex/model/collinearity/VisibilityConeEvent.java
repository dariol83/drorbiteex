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
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;

import java.time.Instant;
import java.util.Date;

public class VisibilityConeEvent {

    private final GroundStation groundStation;
    private final Orbit targetOrbit;
    private final Instant time;
    private final TrackPoint targetPoint;
    private final double angularSeparation;

    public VisibilityConeEvent(GroundStation groundStation, Orbit targetOrbit, Instant time, TrackPoint targetPoint, double angularSeparation) {
        this.groundStation = groundStation;
        this.targetOrbit = targetOrbit;
        this.time = time;
        this.targetPoint = targetPoint;
        this.angularSeparation = angularSeparation;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Orbit getTargetOrbit() {
        return targetOrbit;
    }

    public Instant getTime() {
        return time;
    }

    public TrackPoint getTargetPoint() {
        return targetPoint;
    }

    public double getAngularSeparation() {
        return angularSeparation;
    }

    public String toCSV() {
        return this.groundStation.getName() + ","
                + TimeUtils.formatDate(new Date(this.time.toEpochMilli())) + ","
                + targetOrbit.getName() + ","
                + targetPoint.getAzimuth() + ","
                + targetPoint.getElevation() + ","
                + String.format("%.2f", angularSeparation);
    }

    public static String getCsvHeader() {
        return "Ground Station, Time, Target Orbit, Target AZ, Target EL, Angular Separation";
    }

    @Override
    public String toString() {
        return "CollinearityEvent{" +
                "groundStation=" + groundStation +
                ", targetOrbit=" + targetOrbit +
                ", time=" + time +
                ", targetPoint=" + targetPoint +
                ", angularSeparation=" + angularSeparation +
                '}';
    }
}
