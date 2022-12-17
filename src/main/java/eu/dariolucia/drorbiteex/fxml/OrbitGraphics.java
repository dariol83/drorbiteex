/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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
import eu.dariolucia.drorbiteex.model.orbit.IOrbitListener;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitManager;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrbitGraphics implements IOrbitListener {

    private final ModelManager manager;
    private final Orbit obj;

    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    private Group graphicItem;
    private Text textItem;
    private Box scItem;
    private Group groupItem;

    public OrbitGraphics(ModelManager manager, Orbit obj) {
        this.manager = manager;
        this.obj = obj;
        this.obj.addListener(this);
        this.visibleProperty.set(obj.isVisible());
        this.visibleProperty.addListener((source,oldV,newV) -> BackgroundThread.runLater(() -> obj.setVisible(newV)));
        this.selectedProperty.addListener((source,oldV,newV) -> updateOrbitColor(newV));
    }

    private void updateOrbitColor(boolean selected) {
        Color c = selected ? Color.valueOf(obj.getColor()).brighter() : Color.valueOf(obj.getColor());
        updateElementsColor(c);

    }

    private void updateElementsColor(Color c) {
        PhongMaterial pm = new PhongMaterial(c);
        if(!this.graphicItem.getChildren().isEmpty()) {
            Group orbitGroup = (Group) this.graphicItem.getChildren().get(0);
            for(Node n : orbitGroup.getChildren()) {
                Cylinder cil = (Cylinder) n;
                cil.setMaterial(pm);
            }
        }
        this.textItem.setStroke(c);
        this.scItem.setMaterial(pm);
    }

    public SimpleBooleanProperty visibleProperty() {
        return visibleProperty;
    }

    public SimpleBooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public String getName() {
        return obj.getName();
    }

    public final Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        List<Node> toAdd = constructGraphicItems();
        updateGraphicItems(true);
        this.groupItem = new Group(toAdd);
        this.groupItem.visibleProperty().bind(this.visibleProperty);
        return this.groupItem;
    }

    public Group getGraphicItem() {
        return graphicItem;
    }

    private void updateGraphicItems(boolean renderTrajectory) {
        if(renderTrajectory) {
            renderTrajectory();
        }
        renderSpacecraftLocation();
    }

    private void renderTrajectory() {
        Color c = Color.valueOf(obj.getColor());
        if(selectedProperty.get()) {
            c = c.brighter();
        }
        // Draw trajectory
        List<SpacecraftPosition> trajectory = obj.getSpacecraftPositions();
        // Transform all points to line
        List<Point3D> scPoints = trajectory.stream().map(this::transform).collect(Collectors.toList());
        this.graphicItem.getChildren().clear();
        this.graphicItem.getChildren().add(DrawingUtils.createLine(scPoints, c));
    }

    private void renderSpacecraftLocation() {
        Color c = Color.valueOf(obj.getColor());
        if(selectedProperty.get()) {
            c = c.brighter();
        }
        // Draw SC position
        SpacecraftPosition currentPosition = obj.getCurrentSpacecraftPosition();
        if(currentPosition == null) {
            return;
        }
        Point3D scLocation = transform(currentPosition);
        this.scItem.setMaterial(new PhongMaterial(c));
        this.scItem.getTransforms().clear();
        this.scItem.getTransforms().add(new Translate(scLocation.getX(), scLocation.getY(), scLocation.getZ()));

        // Set spacecraft text where it is now
        Transform result = new Translate(scLocation.getX() * 1.05, scLocation.getY() * 1.05, scLocation.getZ() * 1.05);
        result = result.createConcatenation(new Rotate(Math.toDegrees(currentPosition.getLatLonHeight().getLongitude()), new Point3D(0, -1, 0)));
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);
        this.textItem.setText(obj.getCode());
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(c);
    }

    private Point3D transform(SpacecraftPosition ss) {
        Vector3D position = ss.getPositionVector();
        // ECEF to screen
        return new Point3D(position.getY() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getZ() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getX() * DrawingUtils.EARTH_SCALE_FACTOR);
    }

    private List<Node> constructGraphicItems() {
        // Trajectory object
        this.graphicItem = new Group();
        // Spacecraft object
        this.scItem = new Box(5,5,5);
        // Spacecraft text
        this.textItem = new Text(0, 0, obj.getCode());

        return Arrays.asList(graphicItem, scItem, textItem);
    }

    public void draw(GraphicsContext gc, ViewBox widgetViewport, ViewBox latLonViewport, boolean isSelected) {
        if(obj.isVisible()) {
            List<SpacecraftPosition> spacecraftPositions = obj.getSpacecraftPositions();
            List<double[]> latLonPoints = spacecraftPositions.stream().map(o -> new double[] {Math.toDegrees(o.getLatLonHeight().getLatitude()), Math.toDegrees(o.getLatLonHeight().getLongitude())}).collect(Collectors.toList());
            if(!isSelected) {
                gc.setStroke(Color.valueOf(obj.getColor()));
                gc.setFill(gc.getStroke());
                gc.setLineWidth(1.5);
            } else {
                gc.setStroke(Color.valueOf(obj.getColor()).brighter());
                gc.setFill(gc.getStroke());
                gc.setLineWidth(3.5);
            }
            if (!latLonPoints.isEmpty()) {
                double[] previousPoint = latLonPoints.get(0);
                double[] start = DrawingUtils.mapToWidgetCoordinates(previousPoint[0], previousPoint[1], widgetViewport, latLonViewport);
                gc.beginPath();
                gc.moveTo(start[0], start[1]);
                for (int i = 1; i < latLonPoints.size(); ++i) {
                    double[] nextPoint = latLonPoints.get(i);
                    double[] p2 = DrawingUtils.mapToWidgetCoordinates(nextPoint[0], nextPoint[1], widgetViewport, latLonViewport);
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
            if(obj.getCurrentSpacecraftPosition() == null) {
                return;
            }
            GeodeticPoint scLatLon = obj.getCurrentSpacecraftPosition().getLatLonHeight();
            if (scLatLon != null) {
                double[] scCenter = DrawingUtils.mapToWidgetCoordinates(Math.toDegrees(scLatLon.getLatitude()), Math.toDegrees(scLatLon.getLongitude()), widgetViewport, latLonViewport);
                if(isSelected) {
                    gc.fillRect(scCenter[0] - 4, scCenter[1] - 4, 8, 8);
                } else {
                    gc.fillRect(scCenter[0] - 2, scCenter[1] - 2, 4, 4);
                }
                gc.fillText(obj.getCode(), scCenter[0], scCenter[1] - 5);
            }
        }
    }

    public final void dispose() {
        this.groupItem.visibleProperty().unbind();
        this.graphicItem = null;
        this.textItem = null;
        this.scItem = null;
        this.groupItem.getChildren().clear();
        this.groupItem = null;
    }

    @Override
    public void orbitAdded(OrbitManager manager, Orbit orbit) {
        // Do nothing here
    }

    @Override
    public void orbitRemoved(OrbitManager manager, Orbit orbit) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(this::dispose);
        }
    }

    @Override
    public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(() -> updateGraphicItems(true));
        }
    }

    @Override
    public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(() -> updateGraphicItems(false));
        }
    }

    public Orbit getOrbit() {
        return obj;
    }

    @Override
    public String toString() {
        return this.obj.getCode();
    }
}
