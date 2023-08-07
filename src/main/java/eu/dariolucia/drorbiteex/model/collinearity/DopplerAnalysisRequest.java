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

import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.util.Date;
import java.util.List;

public class DopplerAnalysisRequest {

    private final List<TrackPoint> trackPoints;
    private final Date startTime;
    private final Date endTime;
    private final double frequency;

    public DopplerAnalysisRequest(VisibilityWindow window, double frequency) {
        if(window.getGroundTrack().isEmpty()) {
            throw new RuntimeException("Empty visibility window ground track");
        }
        this.startTime = window.getGroundTrack().get(0).getTime();
        this.endTime = window.getGroundTrack().get(window.getGroundTrack().size() - 1).getTime();
        // Copy the main information
        this.trackPoints = List.copyOf(window.getGroundTrack());
        this.frequency = frequency;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public double getFrequency() {
        return frequency;
    }

    public List<TrackPoint> getTrackPoints() {
        return trackPoints;
    }

    public String getOrbitName() {
        return this.trackPoints.get(0).getSpacecraftPosition().getOrbit().getName();
    }
}
