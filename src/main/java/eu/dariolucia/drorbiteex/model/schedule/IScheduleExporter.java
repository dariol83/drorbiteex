/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Exporter implementation for CCSDS Simple Schedule files.
 */
public interface IScheduleExporter {

    /**
     * Name of the implementation.
     *
     * @return implementation name
     */
    String getName();

    LinkedHashMap<String, String> getSimpleScheduleRootAttributes();

    String getScheduledPackageIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit);

    String getScheduledPackageCommentFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledPackageUserFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledPackageOriginatingRequestIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledActivityIdFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window);

    ServicePackageXRef getServicePackageXRefFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId, String comment, String user);

    ActivityStatusEnum getScheduledActivityStatusFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId);

    List<ScheduledActivityParameter> getScheduledActivityParameterFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId);

}
