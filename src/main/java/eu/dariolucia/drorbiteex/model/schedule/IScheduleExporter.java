package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.util.List;

public interface IScheduleExporter {

    String getName();

    String getScheduledPackageIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit);

    String getScheduledPackageCommentFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledPackageUserFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledPackageOriginatingRequestIdFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId);

    String getScheduledActivityIdFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window);

    ServicePackageXRef getServicePackageXRefFor(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, String scheduledPackageId, String comment, String user);

    ActivityStatusEnum getScheduledActivityStatusFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId);

    List<ScheduledActivityParameter> getScheduledActivityParameterFor(ScheduleGenerationRequest request, GroundStation station, VisibilityWindow window, List<ServiceInfoRequest> requests, String scheduledPackageId, String scheduledActivityId);

}
