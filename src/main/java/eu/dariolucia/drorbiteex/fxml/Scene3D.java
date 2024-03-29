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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.orekit.bodies.GeodeticPoint;

import java.net.URL;
import java.util.ResourceBundle;

public class Scene3D implements Initializable {

    public SubScene scene3d;

    // Root node
    private Group group;
    // Earth node
    private Group earth;

    // For dragging purposes
    private boolean dragging;
    private double dragXStart;
    private double dragYStart;

    private double initialYangle = 0;
    private double initialXangle = 0;

    private double currentYangle = 0;
    private double currentXangle = 0;

    private int zoomFactor = 0;
    private double zoomDeltaFactor = 0.4;

    // Ground stations node
    private Group groundStationGroup;

    // Orbits node
    private Group orbitGroup;
    private OrbitGraphics trackedOrbit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Earth sphere object
        earth = DrawingUtils.createEarthSphere();
        groundStationGroup = new Group();
        orbitGroup = new Group();
        group = new Group(earth, groundStationGroup, orbitGroup);

        // Handle 3D view
        scene3d.setFill(Color.TRANSPARENT);
        scene3d.setRoot(group);
        scene3d.setDepthTest(DepthTest.ENABLE);
        scene3d.setManaged(false);
        PerspectiveCamera pc = new PerspectiveCamera();
        pc.setNearClip(0.05);
        scene3d.setCamera(pc);
        scene3d.heightProperty().addListener((a,b,c) -> group.setTranslateY(c.floatValue()/2));
        scene3d.widthProperty().addListener((a,b,c) -> group.setTranslateX(c.floatValue()/2));

        scene3d.addEventHandler(ScrollEvent.SCROLL, this::onScrollOnScene);
        scene3d.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onStartDragOnScene);
        scene3d.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragOnScene);
        scene3d.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onEndDragOnScene);

    }

    public void setBackgroundColor(Color c) {
        scene3d.setFill(c);
    }

    public void configure(Region parentRegion) {
        scene3d.heightProperty().unbind();
        scene3d.widthProperty().unbind();

        scene3d.heightProperty().bind(parentRegion.heightProperty());
        scene3d.widthProperty().bind(parentRegion.widthProperty());
    }

    private void onEndDragOnScene(MouseEvent t) {
        if(this.trackedOrbit != null) {
            // Tracking, do not do anything
            return;
        }
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = false;
            dragXStart = 0;
            dragYStart = 0;
            initialYangle = currentYangle;
            initialXangle = currentXangle;
            currentYangle = 0;
            currentXangle = 0;
        }
    }

    private void onDragOnScene(MouseEvent t) {
        if(this.trackedOrbit != null) {
            // Tracking, do not do anything
            return;
        }
        if(t.getButton() == MouseButton.PRIMARY && dragging) {
            // compute delta
            double deltaX = t.getSceneX() - dragXStart;
            double deltaY = t.getSceneY() - dragYStart;
            // compute new roll and pitch
            currentXangle = initialXangle + deltaX * zoomDeltaFactor;
            currentYangle = initialYangle + deltaY * zoomDeltaFactor;
            // set rotation
            Rotate xRotation = new Rotate(currentXangle, new Point3D(0,-1, 0));
            Point3D yAxis = new Point3D(Math.cos(Math.toRadians(-currentXangle)),0, Math.sin(Math.toRadians(-currentXangle)));
            Rotate yRotation = new Rotate(currentYangle, yAxis);
            Transform result = xRotation.createConcatenation(yRotation);
            group.getTransforms().clear();
            group.getTransforms().add(result);
        }
    }

    public void moveToLonLat(double lon, double lat) {
        // lon/lat are in degrees
        currentXangle = lon;
        currentYangle = lat;
        // set rotation
        Rotate xRotation = new Rotate(currentXangle, new Point3D(0,-1, 0));
        Point3D yAxis = new Point3D(Math.cos(Math.toRadians(-currentXangle)),0, Math.sin(Math.toRadians(-currentXangle)));
        Rotate yRotation = new Rotate(currentYangle, yAxis);
        Transform result = xRotation.createConcatenation(yRotation);
        group.getTransforms().clear();
        group.getTransforms().add(result);
    }

    private void onStartDragOnScene(MouseEvent t) {
        if(this.trackedOrbit != null) {
            // Tracking, do not do anything
            return;
        }
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = true;
            dragXStart = t.getSceneX();
            dragYStart = t.getSceneY();
        }
    }

    private void onScrollOnScene(ScrollEvent event) {
        // Prevent super zoom in that cuts the scene
        if(group.getTranslateZ() <= -1300 && event.getDeltaY() > 0) {
            return;
        }
        int zoomDelta;
        if(event.getDeltaY() < 0) {
            zoomDelta =  group.getTranslateZ() < -1200.0 ? 10 : 50;
            zoomFactor += 1;
            group.setTranslateZ(group.getTranslateZ() + zoomDelta);
        } else if(event.getDeltaY() > 0) {
            zoomDelta =  group.getTranslateZ() <= -1200.0 ? 10 : 50;
            group.setTranslateZ(group.getTranslateZ() - zoomDelta);
            zoomFactor -= 1;
        }
        if(zoomFactor >= -2) {
            zoomDeltaFactor = 0.4;
        } else {
            zoomDeltaFactor = 0.1;
        }
    }

    public void registerNewGroundStation(GroundStationGraphics graphics) {
        Group s = graphics.createGraphicItem();
        groundStationGroup.getChildren().add(s);
    }

    public void deregisterGroundStation(GroundStationGraphics graphics) {
        groundStationGroup.getChildren().remove(graphics.getGraphicItem());
    }

    public void registerNewOrbit(OrbitGraphics graphics) {
        Group s = graphics.createGraphicItem();
        orbitGroup.getChildren().add(s);
    }

    public void deregisterOrbit(OrbitGraphics graphics) {
        orbitGroup.getChildren().remove(graphics.getGraphicItem());
        if(graphics.equals(this.trackedOrbit)) {
            activateTracking(null);
        }
    }

    public Node getMainScene() {
        return this.scene3d;
    }

    public void activateTracking(OrbitGraphics o) {
        this.trackedOrbit = o;
        if(o != null) {
            GeodeticPoint latLonHeight = o.getOrbit().getCurrentSpacecraftPosition().getLatLonHeight();
            moveToLonLat(-Math.toDegrees(latLonHeight.getLongitude()), Math.toDegrees(latLonHeight.getLatitude()));
        }
    }

    public void updateIfTrackingOrbit(Orbit orbit, SpacecraftPosition currentPosition) {
        if(this.trackedOrbit != null && this.trackedOrbit.getOrbit().equals(orbit)) {
            // 3D scene is tracking, update lon/lat
            GeodeticPoint latLonHeight = currentPosition.getLatLonHeight();
            moveToLonLat(-Math.toDegrees(latLonHeight.getLongitude()), Math.toDegrees(latLonHeight.getLatitude()));
        }
    }
}
