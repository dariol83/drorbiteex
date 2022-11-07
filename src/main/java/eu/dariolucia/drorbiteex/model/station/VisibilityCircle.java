package eu.dariolucia.drorbiteex.model.station;

import org.orekit.bodies.GeodeticPoint;

import java.util.List;

public class VisibilityCircle {

    private final List<GeodeticPoint> visibilityCircle;
    private final boolean polarCircle;

    public VisibilityCircle(List<GeodeticPoint> visibilityCircle) {
        this.visibilityCircle = List.copyOf(visibilityCircle);
        this.polarCircle = isPolarVisibilityCircle(this.visibilityCircle);
    }

    public List<GeodeticPoint> getVisibilityCircle() {
        return visibilityCircle;
    }

    public boolean isPolarCircle() {
        return polarCircle;
    }

    private boolean isPolarVisibilityCircle(List<GeodeticPoint> visibilityCircle) {
        for(int i = 0; i < visibilityCircle.size(); ++i) {
            if(i < visibilityCircle.size() - 1) {
                if(Math.abs(visibilityCircle.get(i).getLongitude() - visibilityCircle.get(i + 1).getLongitude()) > Math.PI + 0.1) {
                    return true;
                }
            } else {
                if(Math.abs(visibilityCircle.get(i).getLongitude() - visibilityCircle.get(0).getLongitude()) > Math.PI + 0.1) {
                    return true;
                }
            }
        }
        return false;
    }
}
