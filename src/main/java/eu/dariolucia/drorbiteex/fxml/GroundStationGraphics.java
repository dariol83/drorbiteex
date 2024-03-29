/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.station.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.util.*;
import java.util.stream.Collectors;

public class GroundStationGraphics implements IGroundStationListener {

    private static final int GS_RADIUS = 1;

    private final GroundStation obj;
    private final ModelManager manager;

    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty visibilityLineProperty = new SimpleBooleanProperty(false);

    private transient Sphere graphicItem;
    private transient Group visibilityItem;

    private transient Map<Orbit, Cylinder> orbit2visibility = new HashMap<>();
    private transient Text textItem;
    private transient Group groupItem;

    public GroundStationGraphics(ModelManager manager, GroundStation obj) {
        this.manager = manager;
        this.obj = obj;
        this.obj.addListener(this);
        this.visibleProperty.set(obj.isVisible());
        this.visibleProperty.addListener((source,oldV,newV) -> BackgroundThread.runLater(() -> obj.setVisible(newV)));
        this.visibilityLineProperty.addListener((source, oldV, newV) -> updateVisibility());
    }

    private void updateVisibility() {
        for (Map.Entry<Orbit, Cylinder> e : orbit2visibility.entrySet()) {
            // Override
            e.getValue().setVisible(visibilityLineProperty.get() && visibleProperty.get() && e.getKey().isVisible());
        }
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

    public void draw(GraphicsContext gc, OrbitGraphics selectedOrbit, ViewBox widgetViewport, ViewBox latLonViewport, boolean isSelected) {
        if(obj.isVisible()) {
            double[] xy = DrawingUtils.mapToWidgetCoordinates(obj.getLatitude(), obj.getLongitude(), widgetViewport, latLonViewport);
            Color gsColor = Color.valueOf(obj.getColor());
            if(isSelected) {
                gsColor = gsColor.brighter();
            }
            gc.setFill(gsColor);
            gc.setStroke(gsColor);
            gc.fillOval(xy[0] - 2, xy[1] - 2, 4, 4);
            if(isSelected) {
                gc.strokeOval(xy[0] - 4, xy[1] - 4, 8, 8);
            }
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
                    List<double[]> toRender = visibilityCircleSortedLatLon.stream().map(gp -> DrawingUtils.mapToWidgetCoordinates(gp[0], gp[1], widgetViewport, latLonViewport)).collect(Collectors.toCollection(ArrayList::new));
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
                            gc.lineTo(widgetViewport.getEndX(), widgetViewport.getStartY()); // top right
                            gc.lineTo(widgetViewport.getStartX(), widgetViewport.getStartY()); // top left
                        } else {
                            gc.lineTo(widgetViewport.getEndX(), widgetViewport.getEndY()); // bottom right
                            gc.lineTo(widgetViewport.getStartX(), widgetViewport.getEndY()); // bottom left
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
        if(!groundStation.equals(this.obj)) {
            return;
        }
        Platform.runLater(() -> {
            // Remove the old line
            Cylinder visibilityLine = this.orbit2visibility.remove(orbit);
            if(visibilityLine != null) {
                this.visibilityItem.getChildren().remove(visibilityLine);
            }
            // Add the new line, in case the elevation justifies it
            if(point != null && point.getElevation() > 0) {
                // Satellite position
                Point3D scPos = transform(point.getSpacecraftPosition());
                Point3D gsPos = DrawingUtils.latLonToScreenPoint(obj.getLatitude(), obj.getLongitude(), DrawingUtils.EARTH_RADIUS);
                Cylinder connection = DrawingUtils.createConnection(scPos, gsPos, Color.valueOf(obj.getColor()), 0.2);
                connection.setVisible(visibilityLineProperty.get() && visibleProperty.get() && orbit.isVisible());
                this.visibilityItem.getChildren().add(connection);
                this.orbit2visibility.put(orbit, connection);
            }
        });
    }

    private Point3D transform(SpacecraftPosition ss) {
        Vector3D position = ss.getPositionVector();
        // ECEF to screen
        return new Point3D(position.getY() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getZ() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getX() * DrawingUtils.EARTH_SCALE_FACTOR);
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

    public SimpleBooleanProperty visibilityLineProperty() {
        return visibilityLineProperty;
    }

    private static final Comparator<double[]> LONGITUDE_SORTER = (o1, o2) -> {
        if(o1[0] == o2[0]) {
            return Double.compare(o1[1], o2[1]);
        } else {
            return Double.compare(o1[0], o2[0]);
        }
    };

    public void informOrbitUpdated(Orbit orbit) {
        Cylinder visibilityLine = this.orbit2visibility.get(orbit);
        if(visibilityLine != null) {
            visibilityLine.setVisible(visibilityLineProperty.get() && visibleProperty.get() && orbit.isVisible());
        }
    }

    public void informOrbitRemoved(Orbit orbit) {
        Cylinder visibilityLine = this.orbit2visibility.get(orbit);
        if(visibilityLine != null) {
            visibilityLine = this.orbit2visibility.remove(orbit);
            this.visibilityItem.getChildren().remove(visibilityLine);
        }
    }
}
