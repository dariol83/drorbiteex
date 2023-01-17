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

package eu.dariolucia.drorbiteex.model.station;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStationParameterConfiguration {

    private int elevationThreshold = 5;
    private int trackingInterval = 10;

    public GroundStationParameterConfiguration() {
    }

    public GroundStationParameterConfiguration(int elevationThreshold, int trackingInterval) {
        this.elevationThreshold = elevationThreshold;
        this.trackingInterval = trackingInterval;
    }

    public void update(GroundStationParameterConfiguration p) {
        this.elevationThreshold = p.elevationThreshold;
        this.trackingInterval = p.trackingInterval;
    }

    public int getElevationThreshold() {
        return elevationThreshold;
    }

    public void setElevationThreshold(int elevationThreshold) {
        this.elevationThreshold = elevationThreshold;
    }

    public int getTrackingInterval() {
        return trackingInterval;
    }

    public void setTrackingInterval(int trackingInterval) {
        this.trackingInterval = trackingInterval;
    }

    public GroundStationParameterConfiguration copy() {
        return new GroundStationParameterConfiguration(this.elevationThreshold, this.trackingInterval);
    }

    @Override
    public String toString() {
        return "GroundStationParameterConfiguration{" +
                "elevationThreshold=" + elevationThreshold +
                "trackingInterval=" + trackingInterval +
                '}';
    }
}
