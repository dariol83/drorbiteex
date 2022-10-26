package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
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
import org.orekit.utils.PVCoordinates;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.*;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStation implements EventHandler<ElevationDetector> {

    private static final int GS_RADIUS = 1;
    private static final double MAX_CHECK = 60.0;
    private static final double THRESHOLD =  0.001;
    private static final double GS_ELEVATION = Math.toRadians(0);

    private String code;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private double height;
    private String color;
    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Sphere graphicItem;
    private transient Group visibilityItem;
    private transient Text textItem;
    private transient Group groupItem;

    private transient volatile GeodeticPoint geodeticPoint;
    private transient volatile TopocentricFrame stationFrame;

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
    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
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
        this.visibilityItem = new Group();
        updateGraphicParameters();
        this.graphicItem.visibleProperty().bind(this.visibleProperty);
        this.textItem.visibleProperty().bind(this.visibleProperty);
        this.visibilityItem.visibleProperty().bind(this.visibleProperty);
        this.groupItem = new Group(graphicItem, textItem, visibilityItem);
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

    public TopocentricFrame getStationFrame() {
        return this.stationFrame;
    }

    private void updateGraphicParameters() {
        this.geodeticPoint = new GeodeticPoint(Math.toRadians(latitude), Math.toRadians(longitude), getHeight());
        this.stationFrame = new TopocentricFrame(Utils.getEarthShape(), geodeticPoint, getCode());
        this.eventDetector = new ElevationDetector(MAX_CHECK, THRESHOLD, this.stationFrame).withConstantElevation(GS_ELEVATION).withHandler(this);

        PhongMaterial m = new PhongMaterial(Color.valueOf(this.color));
        this.graphicItem.setMaterial(m);
        if(!this.visibilityItem.getChildren().isEmpty()) {
            Color originalFill = Color.valueOf(getColor());
            ((MeshView) this.visibilityItem.getChildren().get(0)).setMaterial(new PhongMaterial(new Color(originalFill.getRed(), originalFill.getGreen(), originalFill.getBlue(), originalFill.getOpacity()/3)));
        }
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
            if(!this.visibilityCircle.isEmpty()) {
                List<double[]> toRender = this.visibilityCircleSortedLatLon.stream().map(gp -> Utils.toXY(gp[0], gp[1], w, h)).collect(Collectors.toCollection(ArrayList::new));
                if(this.polarVisibilityCircle) {
                    // Solution: points ordered according to longitude, draw them, then close the line with the two corners (check latitude)
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
                    double[] p0 = toRender.get(0);
                    gc.beginPath();
                    gc.moveTo(p0[0], p0[1]);
                    for (int i = 1; i < toRender.size(); ++i) {
                        double[] pi = toRender.get(i);
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

    private transient Map<String, List<VisibilityWindow>> visibilityWindows = new TreeMap<>();
    private transient Map<String, VisibilityWindow.TemporaryPoint> temporaryPointMap = new TreeMap<>();
    private transient String currentSpacecraft;
    private transient AbstractOrbit currentOrbit;
    private transient boolean eventRaised;

    @Override
    public void init(SpacecraftState initialState, AbsoluteDate target, ElevationDetector detector) {
        this.temporaryPointMap.clear();
    }

    @Override
    public Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
        this.eventRaised = true;
        if (increasing) {
            System.out.println("\tVisibility on " + detector.getTopocentricFrame().getName() + " of " + currentSpacecraft
                    + " begins at " + s.getDate());
            if(temporaryPointMap.containsKey(currentSpacecraft)) {
                VisibilityWindow.TemporaryPoint p = temporaryPointMap.remove(currentSpacecraft);
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, null, p.getTime(), this, currentOrbit);
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
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, p.getTime(), s.getDate().toDate(TimeScalesFactory.getUTC()), this, currentOrbit);
                visibilityWindows.computeIfAbsent(currentSpacecraft, o -> new ArrayList<>()).add(vw);
            } else {
                // End of pass but no start: pass in progress
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, null, s.getDate().toDate(TimeScalesFactory.getUTC()), this, currentOrbit);
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

    public void initVisibilityComputation(AbstractOrbit orbit) {
        this.currentSpacecraft = orbit.getName();
        this.visibilityWindows.remove(orbit.getName());
        this.currentOrbit = orbit;
        this.eventRaised = false;
    }

    public void endVisibilityComputation(String spacecraftId, SpacecraftState currentSpacecraftLocation) {
        VisibilityWindow.TemporaryPoint tp = this.temporaryPointMap.remove(spacecraftId);
        // Pass start but not complete: null end date
        if(tp != null) {
            VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, tp.getTime(), null, this, currentOrbit);
            visibilityWindows.computeIfAbsent(currentSpacecraft, o -> new ArrayList<>()).add(vw);
        }
        // Check if at least one event was raised: if not, verify current visibility.
        if(!eventRaised) {
            // As from https://www.orekit.org/mailing-list-archives/orekit-users/msg00625.html
            PVCoordinates pv = currentSpacecraftLocation.getFrame().getTransformTo(this.stationFrame, currentSpacecraftLocation.getDate()).transformPVCoordinates(currentSpacecraftLocation.getPVCoordinates());
            Vector3D p = pv.getPosition();
            // Vector3D v = pv.getVelocity();
            // double azimuth   = p.getAlpha();
            // double elevation = p.getDelta();
            // double doppler   = p.normalize().dotProduct(v);
            if(p.getDelta() > GS_ELEVATION) {
                VisibilityWindow vw = new VisibilityWindow(currentSpacecraft, 0, null, null, this, currentOrbit);
                visibilityWindows.computeIfAbsent(currentSpacecraft, o -> new ArrayList<>()).add(vw);
            }
            // TODO: this approach can be used to compute the pass profile from a given ground station of a given spacecraft
        }
        this.currentSpacecraft = null;
        this.currentOrbit = null;
    }

    public static Point2D convertToAzimuthElevation(TopocentricFrame stationFrame, SpacecraftState ss) {
        PVCoordinates pv = ss.getFrame().getTransformTo(stationFrame, ss.getDate()).transformPVCoordinates(ss.getPVCoordinates());
        Vector3D p = pv.getPosition();
        double azimuth   = Math.toDegrees(p.getAlpha());
        if(azimuth < 0) {
            azimuth += 360.0;
        }
        double elevation = p.getDelta();
        return new Point2D(azimuth, Math.toDegrees(elevation));
    }

    public void removeVisibilityOf(String spacecraftId) {
        this.visibilityWindows.remove(spacecraftId);
    }

    public boolean isAnyPassInThePast(Date d) {
        for(List<VisibilityWindow> vwList : this.visibilityWindows.values()) {
            for(VisibilityWindow vw : vwList) {
                if(vw.isInThePast(d)) {
                    return true;
                }
            }
        }
        return false;
    }

    private transient String currentGroundTrackSelection = null;
    private transient double currentGroundTrackSpacecraftHeight = 0;
    private transient List<GeodeticPoint> visibilityCircle = new LinkedList<>();
    private transient List<double[]> visibilityCircleSortedLatLon = new LinkedList<>();
    private transient boolean polarVisibilityCircle = false;

    public void setSpacecraftGroundTrack(String spacecraft, double[] latLonAltitude) {
        if(spacecraft != null) {
            this.currentGroundTrackSelection = spacecraft;
            this.currentGroundTrackSpacecraftHeight = latLonAltitude[2];
            computeCircle(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + this.currentGroundTrackSpacecraftHeight);
        } else {
            this.currentGroundTrackSelection = null;
            this.currentGroundTrackSpacecraftHeight = 0;
            this.visibilityCircle.clear();
            this.visibilityItem.getChildren().clear();
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
            visibilityCircle.add(station.computeLimitVisibilityPoint(radius, azimuth, GS_ELEVATION));
        }

        this.visibilityCircleSortedLatLon = visibilityCircle.stream().map(gp -> new double[] { Math.toDegrees(gp.getLatitude()), Math.toDegrees(gp.getLongitude()) }).collect(Collectors.toCollection(ArrayList::new));
        // Create mesh for 3D map:
        this.visibilityItem.getChildren().clear();
        MeshView visibility = Utils.createVisibilityMesh(getLatitude(), getLongitude(), this.visibilityCircleSortedLatLon, this.currentGroundTrackSpacecraftHeight);
        Color originalFill = Color.valueOf(getColor());
        visibility.setMaterial(new PhongMaterial(new Color(originalFill.getRed(), originalFill.getGreen(), originalFill.getBlue(), originalFill.getOpacity()/3)));
        this.visibilityItem.getChildren().add(visibility);
        // Now check for the 2D map: perform the necessary conversions
        // Pre-test: if the longitude distance between two consecutive points is > Math.PI, then we are closing to the poles
        if(isPolarVisibilityCircle(this.visibilityCircle)) {
            this.polarVisibilityCircle = true;
            Comparator<double[]> comparator = (a,b) -> {
                if(a[1] == b[1]) {
                    return Double.compare(a[0], b[0]);
                } else {
                    return Double.compare(a[1], b[1]);
                }
            };
            this.visibilityCircleSortedLatLon.sort(comparator);
        } else {
            this.polarVisibilityCircle = false;
        }
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
