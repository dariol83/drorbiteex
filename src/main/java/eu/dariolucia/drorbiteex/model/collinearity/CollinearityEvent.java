package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;

import java.time.Instant;
import java.util.Date;

public class CollinearityEvent {

    private final GroundStation groundStation;
    private final Orbit referenceOrbit;

    private final Orbit targetOrbit;

    private final Instant time;

    private final TrackPoint referencePoint;

    private final TrackPoint targetPoint;

    private final double angularSeparation;

    public CollinearityEvent(GroundStation groundStation, Orbit referenceOrbit, Orbit targetOrbit, Instant time, TrackPoint referencePoint, TrackPoint targetPoint, double angularSeparation) {
        this.groundStation = groundStation;
        this.referenceOrbit = referenceOrbit;
        this.targetOrbit = targetOrbit;
        this.time = time;
        this.referencePoint = referencePoint;
        this.targetPoint = targetPoint;
        this.angularSeparation = angularSeparation;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Orbit getReferenceOrbit() {
        return referenceOrbit;
    }

    public Orbit getTargetOrbit() {
        return targetOrbit;
    }

    public Instant getTime() {
        return time;
    }

    public TrackPoint getReferencePoint() {
        return referencePoint;
    }

    public TrackPoint getTargetPoint() {
        return targetPoint;
    }

    public double getAngularSeparation() {
        return angularSeparation;
    }

    public String toCSV() {
        return this.groundStation.getName() + ","
                + TimeUtils.formatDate(new Date(this.time.toEpochMilli())) + ","
                + referenceOrbit.getName() + ","
                + referencePoint.getAzimuth() + ","
                + referencePoint.getElevation() + ","
                + targetOrbit.getName() + ","
                + targetPoint.getAzimuth() + ","
                + targetPoint.getElevation() + ","
                + String.format("%.2f", angularSeparation);
    }

    @Override
    public String toString() {
        return "CollinearityEvent{" +
                "groundStation=" + groundStation +
                ", referenceOrbit=" + referenceOrbit +
                ", targetOrbit=" + targetOrbit +
                ", time=" + time +
                ", referencePoint=" + referencePoint +
                ", targetPoint=" + targetPoint +
                ", angularSeparation=" + angularSeparation +
                '}';
    }
}
