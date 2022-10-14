package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
    private transient Instant lastTleRenderingTime;

    private transient Group graphicItem;
    private transient Text textItem;
    private transient Box scItem;

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
        // Determine the orbit points
        TLE tleObject = new TLE(this.tle.substring(0, this.tle.indexOf("\n")).trim(), this.tle.substring(this.tle.indexOf("\n")).trim());
        TLEPropagator extrapolator = TLEPropagator.selectExtrapolator(tleObject);
        List<SpacecraftState> scStates = new LinkedList<>();
        AbsoluteDate ad = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
        for(int i = -100; i < 100; ++i) {
            // Propagate for 120 minutes (1 point every 1 minute)
            SpacecraftState next = extrapolator.propagate(ad.shiftedBy(120 * i));
            scStates.add(next);
        }
        // Transform all points to line
        List<Point3D> scPoints = scStates.stream().map(this::transform).collect(Collectors.toList());
        this.graphicItem = Utils.createLine(scPoints, Color.valueOf(getColor()));
        // Set spacecraft where it is now
        this.scItem = new Box(5,5,5);

        SpacecraftState currentLocation = extrapolator.propagate(ad);
        Point3D scLocation = transform(currentLocation);
        this.scItem.setMaterial(new PhongMaterial(Color.valueOf(getColor())));
        this.scItem.setTranslateX(scLocation.getX());
        this.scItem.setTranslateY(scLocation.getY());
        this.scItem.setTranslateZ(scLocation.getZ());

        this.textItem = new Text(0, 0, getCode());
        Transform result = new Translate(scLocation.getX() * 1.05, scLocation.getY() * 1.05, scLocation.getZ() * 1.05);
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);

        return Arrays.asList(graphicItem, scItem, textItem);
    }

    private Point3D transform(SpacecraftState ss) {
        Vector3D position =
                ss.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true)).getPosition();
        // ECEF to screen
        return new Point3D(position.getY() * Utils.EARTH_SCALE_FACTOR,
                - position.getZ() * Utils.EARTH_SCALE_FACTOR,
                - position.getX() * Utils.EARTH_SCALE_FACTOR);
    }

    @Override
    protected void disposeGraphicItems() {
        this.graphicItem = null;
        this.textItem = null;
        this.scItem = null;
    }

    @Override
    public void updateProperties(AbstractOrbit gs) {
        this.tle = ((TleOrbit) gs).getTle();
    }

    @Override
    protected void updateGraphicItems() {
        // TODO

        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(Color.valueOf(getColor()));
        m.setSpecularColor(Color.valueOf(getColor()));
        // this.graphicItem.setMaterial(m);
        // Compute the absolute position of the sphere in the space
        // Point3D location = Utils.latLonToScreenPoint(latitude, longitude, earthRadius);
        // this.graphicItem.setTranslateX(location.getX());
        // this.graphicItem.setTranslateY(location.getY());
        // this.graphicItem.setTranslateZ(location.getZ());
        this.textItem.setText(getCode());
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.valueOf(getColor()));

        // Point3D locationText = Utils.latLonToScreenPoint(latitude, longitude, earthRadius + 10);
        // Transform result = new Translate(locationText.getX(), locationText.getY(), locationText.getZ());
        // this.textItem.getTransforms().clear();
        // this.textItem.getTransforms().add(result);
    }

    @Override
    public void updateOrbitTime(Instant time) {

    }
}
