package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.util.Collections;
import java.util.List;

public class DefaultExporter implements IScheduleExporter {

    @Override
    public String getName() {
        return "Default Exporter";
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
}
