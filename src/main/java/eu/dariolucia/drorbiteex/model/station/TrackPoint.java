package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.SpacecraftPosition;

import java.util.Date;

public class TrackPoint {

    private final Date time;
    private final int orbitNumber;
    private final SpacecraftPosition spacecraftPosition;
    private final GroundStation station;
    private final double elevation;
    private final double azimuth;

    public TrackPoint(Date time, int orbitNumber, SpacecraftPosition spacecraftPosition, GroundStation station, double azimuth, double elevation) {
        this.time = time;
        this.orbitNumber = orbitNumber;
        this.spacecraftPosition = spacecraftPosition;
        this.station = station;
        this.elevation = elevation;
        this.azimuth = azimuth;
    }

    public Date getTime() {
        return time;
    }

    public int getOrbitNumber() {
        return orbitNumber;
    }

    public SpacecraftPosition getSpacecraftPosition() {
        return spacecraftPosition;
    }

    public GroundStation getStation() {
        return station;
    }

    public double getElevation() {
        return elevation;
    }

    public double getAzimuth() {
        return azimuth;
    }
}
