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
import java.util.List;

public class OrbitVisibilityAnalysisRequest extends AbstractTimeIntervalAnalysisRequest {

    private final Orbit referenceOrbit;

    private final List<GroundStation> targetGroundStations;

    private final String exportFolder;

    public OrbitVisibilityAnalysisRequest(Date startTime, Date endTime, Orbit referenceOrbit, List<GroundStation> targetGroundStations, String exportFolder) {
        super(startTime, endTime);
        this.referenceOrbit = referenceOrbit;
        this.targetGroundStations = targetGroundStations;
        this.exportFolder = exportFolder;
    }

    public Orbit getReferenceOrbit() {
        return referenceOrbit;
    }

    public List<GroundStation> getTargetGroundStations() {
        return targetGroundStations;
    }

    public String getExportFolder() {
        return exportFolder;
    }
}
