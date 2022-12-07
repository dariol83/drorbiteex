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

import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import javax.swing.text.View;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class Scene2D implements Initializable {

    private static final double ZOOM_FACTOR_LAT = 5;
    private static final double ZOOM_FACTOR_LON = 10;
    private static final int MIN_ZOOM_FACTOR = 0;
    private static final int MAX_ZOOM_FACTOR = 17;

    // 2D scene (minimap)
    public Canvas scene2d;
    private Image scene2dImage;
    // BMP
    private double widthHeightRatio;
    private ViewBox widgetViewport;
    private Point2D lonLatCenter = new Point2D(0,0);
    private double latRes = 0;
    private double lonRes = 0;
    private ViewBox imageSourceViewport;
    private int zoomFactor = 0;

    // For dragging purposes
    private boolean dragging;
    private double dragXStart;
    private double dragYStart;

    // EMP
    private Supplier<List<OrbitGraphics>> orbitsSupplier;
    private Supplier<List<GroundStationGraphics>> groundStationsSupplier;

    private Supplier<OrbitGraphics> selectedOrbitSupplier;
    private ViewBox latLonViewport;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Update 2D view
        this.scene2dImage = new Image(this.getClass().getResourceAsStream("/images/earth.jpg"));
        this.scene2d.heightProperty().addListener((a,b,c) -> recomputeViewports());
        this.scene2d.widthProperty().addListener((a,b,c) -> recomputeViewports());
        // BMP
        this.widthHeightRatio = this.scene2dImage.getWidth()/this.scene2dImage.getHeight();
        this.widgetViewport = new ViewBox(0, 0, 0, 0);
        this.imageSourceViewport = new ViewBox(0, 0, 0, 0);

        recomputeViewports();

        scene2d.addEventHandler(ScrollEvent.SCROLL, this::onScrollOnScene);
        scene2d.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onStartDragOnScene);
        scene2d.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragOnScene);
        scene2d.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onEndDragOnScene);
        // EMP
    }

    private void onEndDragOnScene(MouseEvent t) {
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = false;
            dragXStart = 0;
            dragYStart = 0;
        }
    }

    private void onDragOnScene(MouseEvent t) {
        // TODO: prevent to scroll outside the boundaries
        if(t.getButton() == MouseButton.PRIMARY && dragging) {
            // compute delta
            double deltaX = t.getSceneX() - dragXStart;
            double deltaY = t.getSceneY() - dragYStart;
            // depending on the zoom factor, compute the new center view position
            double deltaLonX = lonRes * -deltaX;
            double deltaLatY = latRes * deltaY;
            // keep the new point
            dragXStart = t.getSceneX();
            dragYStart = t.getSceneY();
            // move the center
            Point2D newCenter = this.lonLatCenter.add(deltaLonX, deltaLatY);
            if(isInBoundaries(newCenter)) {
                this.lonLatCenter = newCenter;
                recomputeViewports();
            }
        }
    }

    private boolean isInBoundaries(Point2D newCenter) {
        return newCenter.getX() - (180 - zoomFactor * ZOOM_FACTOR_LON) > -180 &&
                newCenter.getX() + (180 - zoomFactor * ZOOM_FACTOR_LON) < 180 &&
                newCenter.getY() + (90 - zoomFactor * ZOOM_FACTOR_LAT) < 90 &&
                newCenter.getY() - (90 - zoomFactor * ZOOM_FACTOR_LAT) > -90;
    }

    private void onStartDragOnScene(MouseEvent t) {
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = true;
            dragXStart = t.getSceneX();
            dragYStart = t.getSceneY();
        }
    }

    private void onScrollOnScene(ScrollEvent event) {
        //
        // Prevent super zoom in that cuts the scene
        if(zoomFactor == MAX_ZOOM_FACTOR && event.getDeltaY() > 0) {
            return;
        }
        // Prevent to zoom out more than needed
        if(zoomFactor == MIN_ZOOM_FACTOR && event.getDeltaY() < 0) {
            return;
        }

        if(event.getDeltaY() > 0) {
            zoomFactor += 1;
        } else if(event.getDeltaY() < 0) {
            zoomFactor -= 1;
        }
        recomputeViewports();
    }

    public void recomputeViewports() {
        System.out.println("Lon Lat Center: " + this.lonLatCenter);

        // latLonViewport should include the complete earth plus bands
        this.latLonViewport = new ViewBox(
                this.lonLatCenter.getX() - (180 - zoomFactor*ZOOM_FACTOR_LON),
                this.lonLatCenter.getY() + (90 - zoomFactor*ZOOM_FACTOR_LAT),
                this.lonLatCenter.getX() + (180 - zoomFactor*ZOOM_FACTOR_LON),
                this.lonLatCenter.getY() - (90 - zoomFactor*ZOOM_FACTOR_LAT)
        );

        // Check ratio
        double widgetRatio = scene2d.getWidth()/scene2d.getHeight();
        if(widgetRatio >= this.widthHeightRatio) {
            // Width/Height >= image ratio => set height, compute/shrink width, translate X (vertical bands): correct
            double newWidth = scene2d.getHeight() * this.widthHeightRatio;
            this.widgetViewport.update((scene2d.getWidth() - newWidth)/2, 0, newWidth + (scene2d.getWidth() - newWidth)/2, scene2d.getHeight());
        } else {
            // Width/Height < image ratio => set width, compute/shrink height, translate Y
            double newHeight = scene2d.getWidth() * (1/this.widthHeightRatio);
            System.out.println("newHeight: " + newHeight + " - scene2d.width: " + scene2d.getWidth() + " - widthHeightRatio: " + this.widthHeightRatio);
            this.widgetViewport.update(0, (scene2d.getHeight() - newHeight) / 2, scene2d.getWidth(), (scene2d.getHeight() - newHeight) / 2 + newHeight);
        }
        // Compute/update resolution
        this.lonRes = this.latLonViewport.getWidth()/this.widgetViewport.getWidth();
        this.latRes = this.latLonViewport.getHeight()/this.widgetViewport.getHeight();

        // Rebuild latLonViewport to match the full widgetViewport
        this.latLonViewport = new ViewBox(
                this.lonLatCenter.getX() - this.lonRes * (this.scene2d.getWidth()/2),
                this.lonLatCenter.getY() + this.latRes * (this.scene2d.getHeight()/2),
                this.lonLatCenter.getX() + this.lonRes * (this.scene2d.getWidth()/2),
                this.lonLatCenter.getY() - this.latRes * (this.scene2d.getHeight()/2)
        );

        // Set the full widgetViewport
        this.widgetViewport.update(0, 0, scene2d.getWidth(), scene2d.getHeight());


        // Check latLonViewport validity: if not valid, update the viewport
        if(this.latLonViewport.getStartX() < -180.0) {
            this.latLonViewport.move(Math.abs(this.latLonViewport.getStartX() + 180.0), 0);
        }

        /*
        if(this.latLonViewport.getStartY() > 90.0) {
            this.latLonViewport.move(0, - Math.abs(this.latLonViewport.getStartY() - 90.0)/2);
        }
        */
        if(this.latLonViewport.getEndX() > 180.0) {
            this.latLonViewport.move(- Math.abs(this.latLonViewport.getEndX() - 180.0), 0);
        }

        /*
        if(this.latLonViewport.getEndY() < -90.0) {
            this.latLonViewport.move(0, Math.abs(this.latLonViewport.getEndY() + 90.0));
        }
        */

        if(this.latLonViewport.getStartY() > 90.0 || this.latLonViewport.getEndY() < -90.0) {
            // move it so that the center of the viewPort is 0, i.e. startY = half of viewport height
            this.latLonViewport.move(0,
                    - this.latLonViewport.getStartY() + this.latLonViewport.getHeight()/2
            ); // TODO: not perfect
        }

        // Recompute the center from the latLonViewport
        Point2D newCenter = new Point2D(
                this.latLonViewport.getStartX() + (this.latLonViewport.getWidth()/2),
                this.latLonViewport.getStartY() - (this.latLonViewport.getHeight()/2)
        );
        // If the center is good, then set it
        if(Double.isFinite(newCenter.getX()) && Double.isFinite(newCenter.getY())) {
            this.lonLatCenter = newCenter;
        }
        System.out.println("Lon Lat Center 2: " + this.lonLatCenter);

        // Compute source viewport
        double tlx, tly, brx, bry;
        tlx = this.scene2dImage.getWidth()/360.0 * (this.latLonViewport.getStartX() + 180);
        tly = this.scene2dImage.getHeight()/180.0 * (90 - this.latLonViewport.getStartY());
        brx = this.scene2dImage.getWidth()/360.0 * (this.latLonViewport.getEndX() + 180);
        bry = this.scene2dImage.getHeight()/180.0 * (90 - this.latLonViewport.getEndY());
        this.imageSourceViewport.update(tlx, tly, brx, bry);

        refreshScene();
    }

    public void configure(Region parentRegion) {
        scene2d.heightProperty().unbind();
        scene2d.widthProperty().unbind();

        scene2d.heightProperty().bind(parentRegion.heightProperty());
        scene2d.widthProperty().bind(parentRegion.widthProperty());
    }

    public void setDataSuppliers(Supplier<List<OrbitGraphics>> orbitsSupplier, Supplier<List<GroundStationGraphics>> groundStationsSupplier, Supplier<OrbitGraphics> selectedOrbitSupplier) {
        this.orbitsSupplier = orbitsSupplier;
        this.groundStationsSupplier = groundStationsSupplier;
        this.selectedOrbitSupplier = selectedOrbitSupplier;
    }

    public void refreshScene() {
        // Handle 2D view
        GraphicsContext gc = scene2d.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, scene2d.getWidth(), scene2d.getHeight());
        drawMap(gc);
        // Clipping
        gc.save();
        gc.beginPath();
        gc.moveTo(widgetViewport.getStartX(), widgetViewport.getStartY());
        gc.lineTo(widgetViewport.getEndX(), widgetViewport.getStartY());
        gc.lineTo(widgetViewport.getEndX(), widgetViewport.getEndY());
        gc.lineTo(widgetViewport.getStartX(), widgetViewport.getEndY());
        gc.lineTo(widgetViewport.getStartX(), widgetViewport.getStartY());
        gc.closePath();
        gc.clip();
        // End clipping
        if(groundStationsSupplier != null && selectedOrbitSupplier != null) {
            for (GroundStationGraphics gs : groundStationsSupplier.get()) {
                gs.draw(gc, selectedOrbitSupplier.get(), widgetViewport, latLonViewport);
            }
        }
        if(orbitsSupplier != null) {
            for (OrbitGraphics gs : orbitsSupplier.get()) {
                gs.draw(gc, widgetViewport, latLonViewport);
            }
        }
        gc.restore();
        // Done
    }

    private void drawMap(GraphicsContext gc) {
        gc.drawImage(this.scene2dImage,
                imageSourceViewport.getStartX(), imageSourceViewport.getStartY(), imageSourceViewport.getWidth(), imageSourceViewport.getHeight(),
                widgetViewport.getStartX(), widgetViewport.getStartY(), widgetViewport.getWidth(), widgetViewport.getHeight());
    }

    public Node getMainScene() {
        return this.scene2d;
    }
}
