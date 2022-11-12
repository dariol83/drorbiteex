package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class VisibilityWindow implements Comparable<VisibilityWindow> {

    private final Orbit orbit;
    private final int orbitNumber;
    private final Date aos;
    private final Date los;
    private final GroundStation station;
    private final List<TrackPoint> azimuthElevationTrack = new LinkedList<>();

    VisibilityWindow(Orbit orbit, int orbitNumber, Date aos, Date los, GroundStation station) {
        this.orbitNumber = orbitNumber;
        this.aos = aos;
        this.los = los;
        this.station = station;
        this.orbit = orbit;
    }

    void initialiseGroundTrack(Orbit orbit, Propagator propagator) {
        if(this.azimuthElevationTrack.isEmpty()) {
            if (propagator != null) {
                Date currentDate = this.aos;
                // If you don't have AOS, then use the current time // TODO: use reference date from parameter
                if (currentDate == null) {
                    currentDate = new Date();
                }
                Date endDate = this.los;
                // If you don't have LOS, then use currentDate + 20 minutes
                if (endDate == null) {
                    endDate = new Date(currentDate.getTime() + 20 * 60 * 1000);
                }
                // Start propagation from currentDate to endDate: 10 seconds interval
                while (currentDate.before(endDate)) {
                    SpacecraftState next = propagator.propagate(new AbsoluteDate(currentDate, TimeScalesFactory.getUTC()));
                    // Convert spacecraft point to azimuth/elevation
                    double[] azElPoint = this.station.getAzimuthElevationOf(next);
                    this.azimuthElevationTrack.add(new TrackPoint(currentDate, new SpacecraftPosition(orbit, this.orbitNumber, next), this.station, azElPoint[0], azElPoint[1]));
                    // Next point, 10 seconds after
                    currentDate = new Date(currentDate.getTime() + 10000);
                }
                // End date
                SpacecraftState next = propagator.propagate(new AbsoluteDate(endDate, TimeScalesFactory.getUTC()));
                // Convert spacecraft point to azimuth/elevation
                double[] azElPoint = this.station.getAzimuthElevationOf(next);
                //
                this.azimuthElevationTrack.add(new TrackPoint(currentDate, new SpacecraftPosition(orbit, this.orbitNumber, next), this.station, azElPoint[0], azElPoint[1]));
            }
        }
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

    public Orbit getOrbit() {
        return orbit;
    }

    @Override
    public String toString() {
        return "VisibilityWindow{" +
                "orbit='" + orbit + '\'' +
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
                return this.orbit.getCode().compareTo(o.getOrbit().getCode());
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
            return TimeUtils.formatDate(this.aos);
        }
    }

    public String getLosString() {
        if(this.los == null) {
            return "---";
        } else {
            return TimeUtils.formatDate(this.los);
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

    public List<TrackPoint> getGroundTrack() {
        return Collections.unmodifiableList(this.azimuthElevationTrack);
    }

    public boolean isInPass(Date time) {
        if(this.aos == null && this.los == null) {
            return true;
        } else if(this.aos == null) {
            return time.before(this.los);
        } else if(this.los == null) {
            return time.after(this.aos);
        } else {
            return time.after(this.aos) && time.before(this.los);
        }
    }
}
