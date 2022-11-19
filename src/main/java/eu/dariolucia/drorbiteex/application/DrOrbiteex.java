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

package eu.dariolucia.drorbiteex.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Optional;

public class DrOrbiteex extends Application {

    public static final String APPLICATION_NAME = "Dr. Orbiteex";

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(DrOrbiteex.class.getResource("/eu/dariolucia/drorbiteex/fxml/Main.fxml"));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(APPLICATION_NAME);
        stage.setWidth(1600);
        stage.setHeight(960);
        Image icon = new Image(DrOrbiteex.class.getResourceAsStream("/satellite-uplink_24.png"));
        stage.getIcons().add(icon);

        stage.setOnCloseRequest(event -> {
            event.consume();
            shutdown();
        });

        stage.show();
    }

    public static void shutdown() {
        if (confirm("Exit " + APPLICATION_NAME, "Exit " + APPLICATION_NAME, "Do you want to close " + APPLICATION_NAME + "?")) {
            Platform.exit();
            System.gc();
            System.exit(0);
        }
    }

    public static boolean confirm(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
