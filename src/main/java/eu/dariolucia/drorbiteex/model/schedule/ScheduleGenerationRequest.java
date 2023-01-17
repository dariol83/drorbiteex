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

package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;

import java.util.Date;
import java.util.List;

public class ScheduleGenerationRequest {

    private final GroundStation groundStation;
    private final List<Orbit> orbits;

    private final Date startTime;
    private final Date endTime;

    private final String originatingRequest;
    private final StatusEnum status;

    private final List<ServiceInfoRequest> serviceInfoRequests;

    private final String exporterToUse;

    private final int startEndActivityDeltaSeconds;

    private final String filePath;

    private final String folderPath;

    private final String generatorToUse;

    public ScheduleGenerationRequest(GroundStation groundStation, List<Orbit> orbits, Date startTime, Date endTime, String originatingRequest, StatusEnum status, List<ServiceInfoRequest> serviceInfoRequests, String exporterToUse, int startEndActivityDeltaSeconds, String filePath, String folderPath, String generatorToUse) {
        this.groundStation = groundStation;
        this.orbits = List.copyOf(orbits);
        this.startTime = startTime;
        this.endTime = endTime;
        this.originatingRequest = originatingRequest;
        this.status = status;
        this.serviceInfoRequests = List.copyOf(serviceInfoRequests);
        this.exporterToUse = exporterToUse;
        this.startEndActivityDeltaSeconds = startEndActivityDeltaSeconds;
        this.filePath = filePath;
        this.folderPath = folderPath;
        this.generatorToUse = generatorToUse;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public List<Orbit> getOrbits() {
        return orbits;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getOriginatingRequest() {
        return originatingRequest;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public List<ServiceInfoRequest> getServiceInfoRequests() {
        return serviceInfoRequests;
    }

    public String getExporterToUse() {
        return exporterToUse;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartEndActivityDeltaSeconds() {
        return startEndActivityDeltaSeconds;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getGeneratorToUse() {
        return generatorToUse;
    }
}
