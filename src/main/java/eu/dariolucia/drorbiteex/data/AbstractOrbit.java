package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class AbstractOrbit {

    private String code;
    private String name;
    private String color;
    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Group groupItem;
    private transient Supplier<List<GroundStation>> groundStationProvider;

    public void setGroundStationProvider(Supplier<List<GroundStation>> groundStationProvider) {
        this.groundStationProvider = groundStationProvider;
    }

    protected List<GroundStation> getGroundStations() {
        if(this.groundStationProvider == null) {
            return Collections.emptyList();
        } else {
            return this.groundStationProvider.get();
        }
    }

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

    public final Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        List<Node> toAdd = constructGraphicItems();
        updateGraphicItems();
        this.groupItem = new Group(toAdd);
        this.groupItem.visibleProperty().bind(this.visibleProperty);
        return this.groupItem;
    }

    protected abstract List<Node> constructGraphicItems();

    public final Group getGraphicItem() {
        return this.groupItem;
    }

    public final void dispose() {
        this.groupItem.visibleProperty().unbind();
        disposeGraphicItems();
        this.groupItem = null;
    }

    protected abstract void disposeGraphicItems();

    public final void update(AbstractOrbit gs) {
        System.out.println("Updating orbit of " + getName());
        this.code = gs.getCode();
        this.name = gs.getName();
        this.color = gs.getColor();

        updateProperties(gs);

        if(this.groupItem != null) {
            updateGraphicItems();
        }
    }

    protected abstract void updateGraphicItems();

    protected abstract void updateProperties(AbstractOrbit gs);

    public abstract void updateOrbitTime(Date time);

    public void draw(GraphicsContext gc, double width, double height) {
        if(isVisible()) {
            List<double[]> latLonPoints = getLatLonPoints();
            gc.setStroke(Color.valueOf(getColor()));
            gc.setFill(Color.valueOf(getColor()));
            gc.setLineWidth(1.5);
            if (!latLonPoints.isEmpty()) {
                double[] previousPoint = latLonPoints.get(0);
                double[] start = Utils.toXY(previousPoint[0], previousPoint[1], width, height);
                gc.beginPath();
                gc.moveTo(start[0], start[1]);
                for (int i = 1; i < latLonPoints.size(); ++i) {
                    double[] nextPoint = latLonPoints.get(i);
                    double[] p2 = Utils.toXY(nextPoint[0], nextPoint[1], width, height);
                    // If there is a longitude sign swap with large distance, moveTo instead of lineTo
                    boolean swap = Math.abs(nextPoint[1] - previousPoint[1]) > 45; // ((nextPoint[1] < 0 && previousPoint[1] > 0) || (nextPoint[1] > 0 && previousPoint[1] < 0)) &&
                    if(swap) {
                        gc.moveTo(p2[0], p2[1]);
                    } else {
                        gc.lineTo(p2[0], p2[1]);
                    }
                    previousPoint = nextPoint;
                }
                gc.stroke();
                gc.closePath();
            }
            double[] scLatLon = getSpacecraftCurrentLatLon();
            if (scLatLon != null) {
                double[] scCenter = Utils.toXY(scLatLon[0], scLatLon[1], width, height);
                gc.fillRect(scCenter[0] - 2, scCenter[1] - 2, 4, 4);
                gc.fillText(getCode(), scCenter[0], scCenter[1] - 5);
            }
        }
    }

    protected abstract List<double[]> getLatLonPoints();

    protected abstract double[] getSpacecraftCurrentLatLon();
}
