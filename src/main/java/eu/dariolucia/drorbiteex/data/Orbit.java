package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Orbit {

    private String code;
    private String name;
    private String tle;

    private String color;
    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Group graphicItem;
    private transient Text textItem;
    private transient Box scItem;
    private transient Group groupItem;

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

    @XmlElement
    public String getTle() {
        return tle;
    }

    public void setTle(String tle) {
        this.tle = tle;
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
        // Determine the orbit points
        TLE tleObject = new TLE(this.tle.substring(0, this.tle.indexOf("\n")).trim(), this.tle.substring(this.tle.indexOf("\n")).trim());
        TLEPropagator extrapolator = TLEPropagator.selectExtrapolator(tleObject);
        List<SpacecraftState> scStates = new LinkedList<>();
        AbsoluteDate ad = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
        // SpacecraftState initialState = extrapolator.getInitialState();
        // scStates.add(initialState);
        for(int i = -100; i < 100; ++i) {
            // Propagate for 120 minutes (1 point every 1 minute)
            // SpacecraftState next = extrapolator.propagate(initialState.getDate().shiftedBy(120 * i));
            SpacecraftState next = extrapolator.propagate(ad.shiftedBy(120 * i));
            scStates.add(next);
        }
        // Transform all points to line
        List<Point3D> scPoints = scStates.stream().map(this::transform).collect(Collectors.toList());
        this.graphicItem = Utils.createLine(scPoints, Color.valueOf(this.color));
        // Set spacecraft where it is now
        this.scItem = new Box(5,5,5);

        SpacecraftState currentLocation = extrapolator.propagate(ad);
        Point3D scLocation = transform(currentLocation);
        this.scItem.setMaterial(new PhongMaterial(Color.valueOf(this.color)));
        this.scItem.setTranslateX(scLocation.getX());
        this.scItem.setTranslateY(scLocation.getY());
        this.scItem.setTranslateZ(scLocation.getZ());

        this.textItem = new Text(0, 0, this.code);
        Transform result = new Translate(scLocation.getX() * 1.1, scLocation.getY() * 1.1, scLocation.getZ() * 1.1);
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);

        updateGraphicParameters();
        this.graphicItem.visibleProperty().bind(this.visibleProperty);
        this.textItem.visibleProperty().bind(this.visibleProperty);
        this.groupItem = new Group(graphicItem, scItem, textItem);
        return this.groupItem;
    }

    private Point3D transform(SpacecraftState ss) {
        // Vector3D position = ss.getPVCoordinates().getPosition();
        Vector3D position =
                ss.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true)).getPosition();
        // ECEF to screen
        Point3D toReturn = new Point3D(position.getY() * Utils.EARTH_SCALE_FACTOR,
                - position.getZ() * Utils.EARTH_SCALE_FACTOR,
                - position.getX() * Utils.EARTH_SCALE_FACTOR);
        return toReturn;
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

    public void update(Orbit gs) {
        this.code = gs.getCode();
        this.name = gs.getName();
        this.tle = gs.getTle();
        this.color = gs.getColor();

        if(this.graphicItem != null) {
            updateGraphicParameters();
        }
    }

    private void updateGraphicParameters() {
        // TODO

        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(Color.valueOf(this.color));
        m.setSpecularColor(Color.valueOf(this.color));
        // this.graphicItem.setMaterial(m);
        // Compute the absolute position of the sphere in the space
        // Point3D location = Utils.latLonToScreenPoint(latitude, longitude, earthRadius);
        // this.graphicItem.setTranslateX(location.getX());
        // this.graphicItem.setTranslateY(location.getY());
        // this.graphicItem.setTranslateZ(location.getZ());
        this.textItem.setText(this.code);
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.valueOf(this.color));

        // Point3D locationText = Utils.latLonToScreenPoint(latitude, longitude, earthRadius + 10);
        // Transform result = new Translate(locationText.getX(), locationText.getY(), locationText.getZ());
        // this.textItem.getTransforms().clear();
        // this.textItem.getTransforms().add(result);
    }
}
