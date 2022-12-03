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

import javafx.beans.property.BooleanProperty;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class Scene2D implements Initializable {

    // 2D scene (minimap)
    public Canvas scene2d;
    private Image scene2dImage;

    private Supplier<List<OrbitGraphics>> orbitsSupplier;
    private Supplier<List<GroundStationGraphics>> groundStationsSupplier;

    private Supplier<OrbitGraphics> selectedOrbitSupplier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Update 2D view
        this.scene2dImage = new Image(this.getClass().getResourceAsStream("/images/earth.jpg"));
        // Minimap alignment
        StackPane.setAlignment(this.scene2d, Pos.BOTTOM_RIGHT);
    }

    public void configure(Supplier<List<OrbitGraphics>> orbitsSupplier, Supplier<List<GroundStationGraphics>> groundStationsSupplier, Supplier<OrbitGraphics> selectedOrbitSupplier) {
        this.orbitsSupplier = orbitsSupplier;
        this.groundStationsSupplier = groundStationsSupplier;
        this.selectedOrbitSupplier = selectedOrbitSupplier;
    }

    public void refreshScene() {
        // Handle 2D view
        GraphicsContext gc = scene2d.getGraphicsContext2D();
        gc.drawImage(this.scene2dImage, 0, 0, scene2d.getWidth(), scene2d.getHeight());
        for(GroundStationGraphics gs : groundStationsSupplier.get()) {
            gs.draw(gc, selectedOrbitSupplier.get(), scene2d.getWidth(), scene2d.getHeight());
        }
        for(OrbitGraphics gs : orbitsSupplier.get()) {
            gs.draw(gc, scene2d.getWidth(), scene2d.getHeight());
        }
        // Done
    }

    public void bindVisibilityTo(BooleanProperty p) {
        this.scene2d.visibleProperty().bind(p);
    }
}
