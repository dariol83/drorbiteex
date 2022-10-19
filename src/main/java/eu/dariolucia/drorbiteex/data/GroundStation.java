package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.ode.events.Action;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.*;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStation implements EventHandler<ElevationDetector> {

    private static final int GS_RADIUS = 1;

    private String code;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String color;
    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Sphere graphicItem;
    private transient Text textItem;
    private transient Group groupItem;

    private transient GeodeticPoint geodeticPoint;
    private transient TopocentricFrame stationFrame;

    private static final double maxcheck  = 60.0;
    private static final double threshold =  0.001;
    private static final double elevation = Math.toRadians(5.0);

    private transient EventDetector eventDetector;


    @XmlAttribute
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAttribute
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @XmlAttribute
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @XmlAttribute
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @XmlAttribute
    public boolean isVisible() {
        return visibleProperty.get();
    }

    public void setVisible(boolean visible) {
        this.visibleProperty.set(visible);
    }

    public SimpleBooleanProperty visibleProperty() {
        return this.visibleProperty;
    }

    @Override
    public String toString() {
        return this.code + " - " + this.name;
    }

    public Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        this.graphicItem = new Sphere(GS_RADIUS);
        this.textItem = new Text(0, 0, this.code);
        updateGraphicParameters();
        this.graphicItem.visibleProperty().bind(this.visibleProperty);
        this.textItem.visibleProperty().bind(this.visibleProperty);
        this.groupItem = new Group(graphicItem, textItem);
        return this.groupItem;
    }

    public Group getGraphicItem() {
        return this.groupItem;
    }

    public void dispose() {
        this.graphicItem.visibleProperty().unbind();
        this.textItem.visibleProperty().unbind();
        this.graphicItem = null;
        this.textItem = null;
        this.groupItem = null;
    }

    public void update(GroundStation gs) {
        this.code = gs.getCode();
        this.name = gs.getName();
        this.description = gs.getDescription();
        this.latitude = gs.getLatitude();
        this.longitude = gs.getLongitude();
        this.color = gs.getColor();

        if(this.graphicItem != null) {
            updateGraphicParameters();
        }
    }

    private void updateGraphicParameters() {
        this.geodeticPoint = new GeodeticPoint(Math.toRadians(latitude), Math.toRadians(longitude), 0);
        this.stationFrame = new TopocentricFrame(Utils.getEarthShape(), geodeticPoint, getCode());
        this.eventDetector = new ElevationDetector(maxcheck, threshold, this.stationFrame).withConstantElevation(elevation).withHandler(this);

        PhongMaterial m = new PhongMaterial(Color.valueOf(this.color));
        this.graphicItem.setMaterial(m);
        // Compute the absolute position of the sphere in the space
        Point3D location = Utils.latLonToScreenPoint(latitude, longitude, Utils.EARTH_RADIUS);
        this.graphicItem.setTranslateX(location.getX());
        this.graphicItem.setTranslateY(location.getY());
        this.graphicItem.setTranslateZ(location.getZ());
        this.textItem.setText(this.code);
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.valueOf(this.color));

        Point3D locationText = Utils.latLonToScreenPoint(latitude, longitude, Utils.EARTH_RADIUS + 10);
        Transform result = new Translate(locationText.getX(), locationText.getY(), locationText.getZ());
        // Rotate depending on longitude, to have a nice rendering
        result = result.createConcatenation(new Rotate(this.longitude, new Point3D(0, -1, 0)));
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);
    }

    public void draw(GraphicsContext gc, double w, double h) {
        if(isVisible()) {
            double[] xy = Utils.toXY(latitude, longitude, w, h);
            gc.setFill(Color.valueOf(getColor()));
            gc.fillOval(xy[0] - 2, xy[1] - 2, 4, 4);
            gc.fillText(getCode(), xy[0], xy[1] - 5);
            // Ground track
            gc.setStroke(Color.valueOf(getColor()));
            Color originalFill = Color.valueOf(getColor());
            gc.setFill(new Color(originalFill.getRed(), originalFill.getGreen(), originalFill.getBlue(), originalFill.getOpacity()/3));
            gc.setLineWidth(1.0);
            if(!visibilityCircle.isEmpty()) {
                // Pretest: if the longitude distance between two consecutive points is > Math.PI, then we are closing to the poles
                // Solution: order the points according to longitude, draw them, then close the line with the two corners (check latitude)
                // TODO: can be computed already when the visibility circle is computed
                if(wideCircle(visibilityCircle)) {
                    Comparator<double[]> comparator = (a,b) -> {
                        if(a[0] == b[0]) {
                            return Double.compare(a[1], b[1]);
                        } else {
                            return Double.compare(a[0], b[0]);
                        }
                    };
                    List<double[]> toRender = visibilityCircle.stream().map(gp -> Utils.toXY(Math.toDegrees(gp.getLatitude()), Math.toDegrees(gp.getLongitude()), w, h)).collect(Collectors.toCollection(ArrayList::new));
                    toRender.sort(comparator);

                    gc.beginPath();
                    double[] p0 = toRender.get(0);
                    // Render from right to left
                    gc.moveTo(p0[0], p0[1]);
                    for (int i = 1; i < toRender.size(); ++i) {
                        double[] pi = toRender.get(i);
                        gc.lineTo(pi[0], pi[1]);
                    }
                    // Now the main line is draw: we have to close it with two corner points
                    if(getLatitude() > 0) {
                        gc.lineTo(w, 0); // top right
                        gc.lineTo(0, 0); // top left
                    } else {
                        gc.lineTo(w, h); // bottom right
                        gc.lineTo(0, h); // bottom left
                    }
                    gc.lineTo(p0[0], p0[1]);
                    gc.stroke();
                    gc.fill();
                    gc.closePath();
                } else {
                    GeodeticPoint gp0 = visibilityCircle.get(0);
                    double[] p0 = Utils.toXY(Math.toDegrees(gp0.getLatitude()), Math.toDegrees(gp0.getLongitude()), w, h);
                    gc.beginPath();
                    gc.moveTo(p0[0], p0[1]);
                    for (int i = 1; i < visibilityCircle.size(); ++i) {
                        GeodeticPoint gpi = visibilityCircle.get(i);
                        double[] pi = Utils.toXY(Math.toDegrees(gpi.getLatitude()), Math.toDegrees(gpi.getLongitude()), w, h);
                        gc.lineTo(pi[0], pi[1]);
                    }
                    gc.lineTo(p0[0], p0[1]);
                    gc.stroke();
                    gc.fill();
                    gc.closePath();
                }
            }

        }
    }

    private boolean wideCircle(List<GeodeticPoint> visibilityCircle) {
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

    private transient Map<String, List<VisibilityWindow>> visibilityWindows = new TreeMap<>();
    private transient Map<String, VisibilityWindow.TemporaryPoint> temporaryPointMap = new TreeMap<>();
    private transient String currentSpacecraft;

    @Override
    public void init(SpacecraftState initialState, AbsoluteDate target, ElevationDetector detector) {
        this.temporaryPointMap.clear();
    }

    @Override
    public Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
        if (increasing) {
            System.out.println("\tVisibility on " + detector.getTopocentricFrame().getName() + " of " + currentSpacecraft
                    + " begins at " + s.getDate());
            if(temporaryPointMap.containsKey(currentSpacecraft)) {
                VisibilityWindow.TemporaryPoint p = temporaryPointMap.remove(currentSpacecraft);
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, null, p.getTime(), this);
                visibilityWindows.computeIfAbsent(currentSpacecraft, o -> new ArrayList<>()).add(vw);
            }
            VisibilityWindow.TemporaryPoint p = new VisibilityWindow.TemporaryPoint(currentSpacecraft, 0, s.getDate().toDate(TimeScalesFactory.getUTC()), true);
            temporaryPointMap.put(currentSpacecraft, p);
            return Action.CONTINUE;
        } else {
            System.out.println("\tVisibility on " + detector.getTopocentricFrame().getName() + " of " + currentSpacecraft
                    + " ends at " + s.getDate());
            if(temporaryPointMap.containsKey(currentSpacecraft)) {
                VisibilityWindow.TemporaryPoint p = temporaryPointMap.remove(currentSpacecraft);
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, p.getTime(), s.getDate().toDate(TimeScalesFactory.getUTC()), this);
                visibilityWindows.computeIfAbsent(currentSpacecraft, o -> new ArrayList<>()).add(vw);
            }
            return Action.CONTINUE;
        }
    }

    @Override
    public SpacecraftState resetState(ElevationDetector detector, SpacecraftState oldState) {
        return oldState;
    }

    public Map<String, List<VisibilityWindow>> getVisibilityWindows() {
        return Map.copyOf(this.visibilityWindows);
    }

    public EventDetector getEventDetector() {
        return this.eventDetector;
    }

    public void initVisibilityComputation(String spacecraftId) {
        this.currentSpacecraft = spacecraftId;
        this.visibilityWindows.remove(spacecraftId);
    }

    public void endVisibilityComputation(String spacecraftId) {
        this.temporaryPointMap.remove(spacecraftId);
        // Ignore last pass if it remains open
        this.currentSpacecraft = null;
    }

    private transient String currentGroundTrackSelection = null;
    private transient double currentGroundTrackSpacecraftHeight = 0;
    private transient List<GeodeticPoint> visibilityCircle = new LinkedList<>();

    public void setSpacecraftGroundTrack(String spacecraft, double[] latLonAltitude) {
        if(spacecraft != null) {
            this.currentGroundTrackSelection = spacecraft;
            this.currentGroundTrackSpacecraftHeight = latLonAltitude[2];
            computeCircle(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + this.currentGroundTrackSpacecraftHeight);
        } else {
            this.currentGroundTrackSelection = null;
            this.currentGroundTrackSpacecraftHeight = 0;
            visibilityCircle.clear();
        }
    }

    private void computeCircle(double radius)
            throws OrekitException {
        // define an array of ground stations
        TopocentricFrame station = this.stationFrame;
        // compute the visibility circle
        visibilityCircle.clear();
        for (int i = 0; i < 180; ++i) {
            double azimuth = i * (2.0 * Math.PI / 180);
            visibilityCircle.add(station.computeLimitVisibilityPoint(radius, azimuth, Math.toRadians(5.0)));
        }
    }
}
