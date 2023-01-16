package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;

import java.util.Date;

public class CollinearityAnalysisRequest {

    private final GroundStation groundStation;
    private final Orbit referenceOrbit;
    private final Date startTime;
    private final Date endTime;
    private final double minAngularSeparation;
    private final String filePath;

    public CollinearityAnalysisRequest(GroundStation groundStation, Orbit referenceOrbit, Date startTime, Date endTime, double minAngularSeparation, String filePath) {
        // Copy the ground station and the reference orbit
        this.groundStation = groundStation.copy();
        this.referenceOrbit = referenceOrbit.copy();
        this.startTime = startTime;
        this.endTime = endTime;
        this.minAngularSeparation = minAngularSeparation;
        this.filePath = filePath;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Orbit getReferenceOrbit() {
        return referenceOrbit;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public double getMinAngularSeparation() {
        return minAngularSeparation;
    }

    public String getFilePath() {
        return filePath;
    }
}
