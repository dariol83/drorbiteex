package eu.dariolucia.drorbiteex.data;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TleOrbit extends AbstractOrbit {

    private String tle;

    private transient boolean tleModifiedSinceLastRendering;
    private transient Date lastTleRenderingTime;

    private transient Group graphicItem;
    private transient Text textItem;
    private transient Box scItem;
    private transient TLEPropagator extrapolator;

    @XmlElement
    public String getTle() {
        return tle;
    }

    public void setTle(String tle) {
        if(this.tle == null || !this.tle.equals(tle)) {
            this.tle = tle;
            this.tleModifiedSinceLastRendering = true;
        }
    }

    @Override
    protected List<Node> constructGraphicItems() {
        // Trajectory object
        this.graphicItem = new Group();
        // Spacecraft object
        this.scItem = new Box(5,5,5);
        // Spacecraft text
        this.textItem = new Text(0, 0, getCode());

        return Arrays.asList(graphicItem, scItem, textItem);
    }

    private Point3D transform(SpacecraftState ss) {
        Vector3D position = ss.getPVCoordinates(Utils.ITRF).getPosition();
        // ECEF to screen
        return new Point3D(position.getY() * Utils.EARTH_SCALE_FACTOR,
                - position.getZ() * Utils.EARTH_SCALE_FACTOR,
                - position.getX() * Utils.EARTH_SCALE_FACTOR);
    }

    private GeodeticPoint toLatLon(SpacecraftState ss) {
        Vector3D position = ss.getPVCoordinates(Utils.ITRF).getPosition();
        return Utils.cartesianToGeodetic(position);
    }

    @Override
    protected void disposeGraphicItems() {
        this.graphicItem = null;
        this.textItem = null;
        this.scItem = null;
    }

    @Override
    public void updateProperties(AbstractOrbit gs) {
        setTle(((TleOrbit) gs).getTle());
    }

    @Override
    protected void updateGraphicItems() {
        if(this.lastTleRenderingTime == null) {
            this.lastTleRenderingTime = new Date();
        }
        updateOrbitTime(this.lastTleRenderingTime);
    }

    @Override
    public void updateOrbitTime(Date time) {
        // Compute and render the TLE propagation when needed
        computeTLEpropagation(time);
    }

    private void computeTLEpropagation(Date time) {
        boolean recomputeTle = this.lastTleRenderingTime == null || time.getTime() - this.lastTleRenderingTime.getTime() > 1800000;
        this.lastTleRenderingTime = time;
        AbsoluteDate ad = new AbsoluteDate(time, TimeScalesFactory.getUTC());
        // Determine the orbit points
        if(recomputeTle || this.tleModifiedSinceLastRendering) {
            System.out.println("Recomputing orbit for satellite " + getName());
            TLE tleObject = new TLE(this.tle.substring(0, this.tle.indexOf("\n")).trim(), this.tle.substring(this.tle.indexOf("\n")).trim());
            this.extrapolator = TLEPropagator.selectExtrapolator(tleObject);
            List<SpacecraftState> scStates = new LinkedList<>();

            for (int i = -100; i < 100; ++i) {
                // Propagate for 400 minutes (1 point every 2 minute - 200 points)
                SpacecraftState next = extrapolator.propagate(ad.shiftedBy(120.0 * i));
                scStates.add(next);
            }
            // Transform all points to line
            List<Point3D> scPoints = scStates.stream().map(this::transform).collect(Collectors.toList());
            this.graphicItem.getChildren().clear();
            this.graphicItem.getChildren().add(Utils.createLine(scPoints, Color.valueOf(getColor())));
            this.tleModifiedSinceLastRendering = false;
        } else {
            // Change only the colour
            Material newMaterial = new PhongMaterial(Color.valueOf(getColor()));
            for(Node n : this.graphicItem.getChildren()) {
                if(n instanceof Group) {
                    for(Node nn : ((Group) n).getChildren()) {
                        if(nn instanceof Cylinder) {
                            ((Cylinder) nn).setMaterial(newMaterial);
                        }
                    }
                }
            }
        }
        // Set spacecraft where it is now
        SpacecraftState currentLocation = extrapolator.propagate(ad);
        Point3D scLocation = transform(currentLocation);
        this.scItem.setMaterial(new PhongMaterial(Color.valueOf(getColor())));
        this.scItem.getTransforms().clear();
        this.scItem.getTransforms().add(new Translate(scLocation.getX(), scLocation.getY(), scLocation.getZ()));

        // Set spacecraft text where it is now
        Transform result = new Translate(scLocation.getX() * 1.05, scLocation.getY() * 1.05, scLocation.getZ() * 1.05);
        result = result.createConcatenation(new Rotate(Math.toDegrees(toLatLon(currentLocation).getLongitude()), new Point3D(0, -1, 0)));
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);

        this.textItem.setText(getCode());
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.valueOf(getColor()));
    }
}
