package eu.dariolucia.drorbiteex.data;

import javafx.geometry.Point2D;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class VisibilityWindow implements Comparable<VisibilityWindow> {

    private final String satellite;
    private final int orbitNumber;
    private final Date aos;
    private final Date los;
    private final GroundStation station;
    private final AbstractOrbit orbit;

    private volatile List<SpacecraftTrackPoint> azimuthElevationTrack = null;

    public VisibilityWindow(String satellite, int orbitNumber, Date aos, Date los, GroundStation station, AbstractOrbit orbit) {
        this.satellite = satellite;
        this.orbitNumber = orbitNumber;
        this.aos = aos;
        this.los = los;
        this.station = station;
        this.orbit = orbit;
    }

    public String getSatellite() {
        return satellite;
    }

    public int getOrbitNumber() {
        return orbitNumber;
    }

    public Date getAos() {
        return aos;
    }

    public Date getLos() {
        return los;
    }

    public GroundStation getStation() {
        return station;
    }

    public AbstractOrbit getOrbit() {
        return orbit;
    }

    @Override
    public String toString() {
        return "VisibilityWindow{" +
                "satellite='" + satellite + '\'' +
                ", orbitNumber=" + orbitNumber +
                ", aos=" + aos +
                ", los=" + los +
                ", station=" + station +
                '}';
    }

    @Override
    public int compareTo(VisibilityWindow o) {
        if(this.aos == null) {
            if(o.aos == null) {
                return this.satellite.compareTo(o.satellite);
            } else {
                return -1;
            }
        } else if(o.aos == null) {
            return 1;
        } else {
            return this.aos.compareTo(o.aos);
        }
    }

    public String getAosString() {
        if(this.aos == null) {
            return "---";
        } else {
            return Utils.formatDate(this.aos);
        }
    }

    public String getLosString() {
        if(this.los == null) {
            return "---";
        } else {
            return Utils.formatDate(this.los);
        }
    }

    public boolean isInThePast(Date d) {
        if(this.aos == null && this.los == null) {
            return false;
        } else if(this.aos == null) {
            // los is not null
            return this.los.before(d);
        } else if(this.los == null) {
            // aos is not null, pass end unknown
            return false;
        } else {
            // aos and los are not null
            return this.los.before(d);
        }
    }

    public List<SpacecraftTrackPoint> getAzimuthElevationTrack() {
        if(this.azimuthElevationTrack == null) {
            Propagator p = this.orbit.getPropagator();
            TopocentricFrame stationFrame = this.station.getStationFrame();
            if(p != null) {
                this.azimuthElevationTrack = new LinkedList<>();
                Date currentDate = this.aos;
                // If you don't have AOS, then use the current time
                if(currentDate == null) {
                    currentDate = new Date();
                }
                Date endDate = this.los;
                // If you don't have LOS, then use currentDate + 20 minutes
                if(endDate == null) {
                    endDate = new Date(currentDate.getTime() + 20 * 60 * 1000);
                }
                // Start propagation from currentDate to endDate: 10 seconds interval
                while(currentDate.before(endDate)) {
                    SpacecraftState next = p.propagate(new AbsoluteDate(currentDate, TimeScalesFactory.getUTC()));
                    // Convert spacecraft point to azimuth/elevation
                    Point2D azElPoint = GroundStation.convertToAzimuthElevation(stationFrame, next);
                    // Adjust
                    azElPoint = Utils.adjustAzimuthElevationPoint(azElPoint);
                    this.azimuthElevationTrack.add(new SpacecraftTrackPoint(currentDate, azElPoint, this));
                    // Next point, 10 seconds after
                    currentDate = new Date(currentDate.getTime() + 10000);
                }
                // End date
                SpacecraftState next = p.propagate(new AbsoluteDate(endDate, TimeScalesFactory.getUTC()));
                // Convert spacecraft point to azimuth/elevation
                Point2D azElPoint = GroundStation.convertToAzimuthElevation(stationFrame, next);
                // Adjust
                azElPoint = Utils.adjustAzimuthElevationPoint(azElPoint);
                //
                this.azimuthElevationTrack.add(new SpacecraftTrackPoint(currentDate, azElPoint, this));
            }
        }
        return this.azimuthElevationTrack;
    }

    public Point2D convertToAzimuthElevation(SpacecraftState currentLocation) {
        TopocentricFrame stationFrame = this.station.getStationFrame();
        Point2D azElPoint = GroundStation.convertToAzimuthElevation(stationFrame, currentLocation);
        azElPoint = Utils.adjustAzimuthElevationPoint(azElPoint);
        return azElPoint;
    }

    public static class SpacecraftTrackPoint {
        private final Date time;
        private final Point2D azimuthElevation;
        private final VisibilityWindow visibilityWindow;

        public SpacecraftTrackPoint(Date time, Point2D azimuthElevation, VisibilityWindow visibilityWindow) {
            this.time = time;
            this.azimuthElevation = azimuthElevation;
            this.visibilityWindow = visibilityWindow;
        }

        public Date getTime() {
            return time;
        }

        public Point2D getAzimuthElevation() {
            return azimuthElevation;
        }

        public VisibilityWindow getVisibilityWindow() {
            return visibilityWindow;
        }
    }

    public static class TemporaryPoint {
        private final String satellite;
        private final int orbitNumber;
        private final Date time;
        private final boolean aos;

        public TemporaryPoint(String satellite, int orbitNumber, Date time, boolean aos) {
            this.satellite = satellite;
            this.orbitNumber = orbitNumber;
            this.time = time;
            this.aos = aos;
        }

        public String getSatellite() {
            return satellite;
        }

        public int getOrbitNumber() {
            return orbitNumber;
        }

        public Date getTime() {
            return time;
        }

        public boolean isAos() {
            return aos;
        }
    }
}
