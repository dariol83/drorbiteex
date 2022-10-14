package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStation {

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
        this.graphicItem = new Sphere(2);
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

    public void update(GroundStation gs, int earthRadius) {
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
        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(Color.valueOf(this.color));
        m.setSpecularColor(Color.valueOf(this.color));
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
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);
    }
}
