package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GroundStationGraphics implements IGroundStationListener {

    private static final int GS_RADIUS = 1;

    private final GroundStation obj;
    private final ModelManager manager;

    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Sphere graphicItem;
    private transient Group visibilityItem;
    private transient Text textItem;
    private transient Group groupItem;

    private transient OrbitGraphics selectedOrbit = null;

    public GroundStationGraphics(ModelManager manager, GroundStation obj) {
        this.manager = manager;
        this.obj = obj;
        this.obj.addListener(this);
        this.visibleProperty.set(obj.isVisible());
        this.visibleProperty.addListener((source,oldV,newV) -> ModelManager.runLater(() -> obj.setVisible(newV)));
    }

    public SimpleBooleanProperty visibleProperty() {
        return visibleProperty;
    }

    public Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        this.graphicItem = new Sphere(GS_RADIUS);
        this.textItem = new Text(0, 0, obj.getCode());
        this.visibilityItem = new Group();
        updateGraphicItems();
        this.graphicItem.visibleProperty().bind(this.visibleProperty);
        this.textItem.visibleProperty().bind(this.visibleProperty);
        this.visibilityItem.visibleProperty().bind(this.visibleProperty);
        this.groupItem = new Group(graphicItem, textItem, visibilityItem);
        return this.groupItem;
    }

    public Group getGraphicItem() {
        return this.groupItem;
    }

    private void updateGraphicItems() {
        PhongMaterial m = new PhongMaterial(Color.valueOf(obj.getColor()));
        this.graphicItem.setMaterial(m);
        if(!this.visibilityItem.getChildren().isEmpty()) {
            Color originalFill = Color.valueOf(obj.getColor());
            ((MeshView) this.visibilityItem.getChildren().get(0)).setMaterial(new PhongMaterial(new Color(originalFill.getRed(), originalFill.getGreen(), originalFill.getBlue(), originalFill.getOpacity()/3)));
        }
        // Compute the absolute position of the sphere in the space
        Point3D location = DrawingUtils.latLonToScreenPoint(obj.getLatitude(), obj.getLongitude(), DrawingUtils.EARTH_RADIUS);
        this.graphicItem.setTranslateX(location.getX());
        this.graphicItem.setTranslateY(location.getY());
        this.graphicItem.setTranslateZ(location.getZ());
        this.textItem.setText(obj.getCode());
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.valueOf(obj.getColor()));

        Point3D locationText = DrawingUtils.latLonToScreenPoint(obj.getLatitude(), obj.getLongitude(), DrawingUtils.EARTH_RADIUS + 10);
        Transform result = new Translate(locationText.getX(), locationText.getY(), locationText.getZ());
        // Rotate depending on longitude, to have a nice rendering
        result = result.createConcatenation(new Rotate(obj.getLongitude(), new Point3D(0, -1, 0)));
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);
    }

    public void setSelectedOrbit(OrbitGraphics selectedOrbit) {
        this.selectedOrbit = selectedOrbit;
        updateVisibilityCircle();
    }

    private void updateVisibilityCircle() {
        if(this.selectedOrbit == null) {
            this.visibilityItem.getChildren().clear();
        } else {
            VisibilityCircle vc = obj.getVisibilityCircleOf(selectedOrbit.getOrbit());
            if (vc != null) {
                List<double[]> visibilityCircleSortedLatLon = vc.getVisibilityCircle().stream().map(gp -> new double[]{Math.toDegrees(gp.getLatitude()), Math.toDegrees(gp.getLongitude())}).collect(Collectors.toCollection(ArrayList::new));
                // Create mesh for 3D map:
                this.visibilityItem.getChildren().clear();
                MeshView visibility = DrawingUtils.createVisibilityMesh(obj.getLatitude(), obj.getLongitude(), visibilityCircleSortedLatLon, selectedOrbit.getOrbit().getCurrentSpacecraftPosition().getLatLonHeight().getAltitude());
                Color originalFill = Color.valueOf(obj.getColor());
                visibility.setMaterial(new PhongMaterial(new Color(originalFill.getRed(), originalFill.getGreen(), originalFill.getBlue(), originalFill.getOpacity() / 3)));
                this.visibilityItem.getChildren().add(visibility);
            } else {
                this.visibilityItem.getChildren().clear();
            }
        }
    }

    public void draw(GraphicsContext gc, OrbitGraphics selectedOrbit, double w, double h) {
        System.out.println("Ground station " + obj + " visible: " + obj.isVisible());
        if(obj.isVisible()) {
            double[] xy = DrawingUtils.toXY(obj.getLatitude(), obj.getLongitude(), w, h);
            Color gsColor = Color.valueOf(obj.getColor());
            gc.setFill(gsColor);
            gc.fillOval(xy[0] - 2, xy[1] - 2, 4, 4);
            gc.fillText(obj.getCode(), xy[0], xy[1] - 5);
            // Ground track
            gc.setStroke(gsColor);
            gc.setFill(new Color(gsColor.getRed(), gsColor.getGreen(), gsColor.getBlue(), gsColor.getOpacity()/3));
            gc.setLineWidth(1.0);
            if(selectedOrbit != null) {
                VisibilityCircle vc = obj.getVisibilityCircleOf(selectedOrbit.getOrbit());
                if (vc != null) {
                    List<double[]> visibilityCircleSortedLatLon = vc.getVisibilityCircle().stream().map(gp -> new double[]{Math.toDegrees(gp.getLatitude()), Math.toDegrees(gp.getLongitude())}).collect(Collectors.toCollection(ArrayList::new));
                    // toXY puts the longitude into [0] and the latitude into [1]
                    List<double[]> toRender = visibilityCircleSortedLatLon.stream().map(gp -> DrawingUtils.toXY(gp[0], gp[1], w, h)).collect(Collectors.toCollection(ArrayList::new));
                    if (vc.isPolarCircle()) {
                        toRender.sort(LONGITUDE_SORTER);
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
                        if (obj.getLatitude() > 0) {
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
    }

    public void dispose() {
        this.obj.removeListener(this);
        this.graphicItem.visibleProperty().unbind();
        this.textItem.visibleProperty().unbind();
        this.graphicItem = null;
        this.textItem = null;
        this.groupItem = null;
    }

    @Override
    public void groundStationAdded(GroundStationManager manager, GroundStation groundStation) {
        // Cannot be called
    }

    @Override
    public void groundStationRemoved(GroundStationManager manager, GroundStation groundStation) {
        if(groundStation.equals(this.obj)) {
            Platform.runLater(this::dispose);
        }
    }

    @Override
    public void groundStationUpdated(GroundStation groundStation) {
        if(groundStation.equals(this.obj)) {
            Platform.runLater(this::updateGraphicItems);
        }
    }

    @Override
    public void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
        if(groundStation.equals(this.obj)) {
            Platform.runLater(this::updateGraphicItems);
        }
    }

    @Override
    public void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        // Nothing
    }

    public GroundStation getGroundStation() {
        return obj;
    }

    public String getName() {
        return obj.getName();
    }

    @Override
    public String toString() {
        return this.obj.getCode();
    }

    private static final Comparator<double[]> LONGITUDE_SORTER = (o1, o2) -> {
        if(o1[0] == o2[0]) {
            return Double.compare(o1[1], o2[1]);
        } else {
            return Double.compare(o1[0], o2[0]);
        }
    };
}
