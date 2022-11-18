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

    public ScheduleGenerationRequest(GroundStation groundStation, List<Orbit> orbits, Date startTime, Date endTime, String originatingRequest, StatusEnum status, List<ServiceInfoRequest> serviceInfoRequests, String exporterToUse, int startEndActivityDeltaSeconds, String filePath) {
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
}
