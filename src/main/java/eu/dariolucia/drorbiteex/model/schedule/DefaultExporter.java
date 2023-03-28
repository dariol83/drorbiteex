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
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.util.*;

public class DefaultExporter implements IScheduleExporter {

    @Override
    public String getName() {
        return "Default Exporter";
    }

    @Override
    public LinkedHashMap<String, String> getSimpleScheduleRootAttributes(GroundStation station) {
        return new LinkedHashMap<>(Map.of(
                "xmlns", "urn:ccsds:schema:cssm:1.0.0",
                "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
                "xmlns:xmi", "http://www.omg.org/XMI",
                "xsi:schemaLocation", "urn:ccsds:schema:cssm:1.0.0 902x01b1TC1-SmplSchd.xsd",
                "srvMgtEntityType", "SIMPLE_SCHEDULE"
        ));
    }

    @Override
    public String getScheduledPackageIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit) {
        return orbit.getCode();
    }

    @Override
    public String getScheduledPackageCommentFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId) {
        return "";
    }

    @Override
    public String getScheduledPackageUserFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId) {
        return "USER";
    }

    @Override
    public String getScheduledPackageOriginatingRequestIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId) {
        return null;
    }

    @Override
    public String getScheduledActivityIdFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window) {
        return window.getOrbit().getCode() + "-" + window.getOrbitNumber();
    }

    @Override
    public ServicePackageXRef getServicePackageXRefFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId, String comment, String user) {
        return null;
    }

    @Override
    public ActivityStatusEnum getScheduledActivityStatusFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId) {
        return ActivityStatusEnum.COMMITTED;
    }

    @Override
    public List<ScheduledActivityParameter> getScheduledActivityParameterFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId) {
        return Collections.emptyList();
    }

    @Override
    public Date getBeginningOfTrackFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, String activityId) {
        return vw.getAos();
    }

    @Override
    public Date getEndOfTrackFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, String activityId) {
        return vw.getLos();
    }

    @Override
    public Date getBeginningOfActivityFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, String activityId) {
        return null;
    }

    @Override
    public Date getEndOfActivityFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, String activityId) {
        return null;
    }

    @Override
    public ServiceInfoParameter getServiceInfoParameterFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, String activityId, ServiceInfoRequest serviceInfoRequest, int serviceIdx, int size, Map<Orbit, List<VisibilityWindow>> allPasses) {
        return null;
    }
}
