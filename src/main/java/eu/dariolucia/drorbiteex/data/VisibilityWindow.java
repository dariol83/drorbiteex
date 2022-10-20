package eu.dariolucia.drorbiteex.data;

import java.util.Date;

public class VisibilityWindow implements Comparable<VisibilityWindow> {

    private final String satellite;
    private final int orbitNumber;
    private final Date aos;
    private final Date los;
    private final GroundStation station;

    public VisibilityWindow(String satellite, int orbitNumber, Date aos, Date los, GroundStation station) {
        this.satellite = satellite;
        this.orbitNumber = orbitNumber;
        this.aos = aos;
        this.los = los;
        this.station = station;
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
