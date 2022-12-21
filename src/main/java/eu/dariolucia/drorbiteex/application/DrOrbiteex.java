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

import eu.dariolucia.drorbiteex.fxml.DialogUtils;
import eu.dariolucia.drorbiteex.fxml.Main;
import eu.dariolucia.drorbiteex.model.ModelManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class DrOrbiteex extends Application {

    public static final String APPLICATION_NAME = "Dr. Orbiteex";

    private static final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + "drorbiteex";
    private static final String DEFAULT_ORBIT_CONFIG_FILE_NAME = "orbits.xml";
    private static final String DEFAULT_ORBIT_CONFIG_LOCATION = DEFAULT_CONFIG_FOLDER + File.separator + DEFAULT_ORBIT_CONFIG_FILE_NAME;
    private static final String DEFAULT_GS_CONFIG_FILE_NAME = "groundstations.xml";
    private static final String DEFAULT_GS_CONFIG_LOCATION = DEFAULT_CONFIG_FOLDER + File.separator + DEFAULT_GS_CONFIG_FILE_NAME;

    private static final String CONFIG_FOLDER_LOCATION_KEY = "drorbiteex.config";
    private static final String OREKIT_FOLDER_NAME = "orekit-data";

    @Override
    public void start(Stage stage) throws Exception {
        Stage substage = new Stage();
        // First the splash
        {
            Parent root = FXMLLoader.load(DrOrbiteex.class.getResource("/eu/dariolucia/drorbiteex/fxml/Splash.fxml"));

            substage.initStyle(StageStyle.UNDECORATED);
            substage.setAlwaysOnTop(true);
            Scene scene = new Scene(root);
            substage.setScene(scene);
            substage.setWidth(400);
            substage.setHeight(300);
            Image icon = new Image(DrOrbiteex.class.getResourceAsStream("/satellite-uplink_24.png"));
            substage.getIcons().add(icon);
            substage.show();
        }
        // New run a one-off thread to load the manager
        new Thread(() -> {
            // Load old configuration if available
            String configLocation = System.getProperty(CONFIG_FOLDER_LOCATION_KEY);
            File orekitData;
            String orbitFile;
            String gsFile;
            if(configLocation != null && !configLocation.isBlank()) {
                orbitFile = configLocation + File.separator + DEFAULT_ORBIT_CONFIG_FILE_NAME;
                gsFile = configLocation + File.separator + DEFAULT_GS_CONFIG_FILE_NAME;
                orekitData = new File(configLocation + File.separator + OREKIT_FOLDER_NAME);
            } else {
                orbitFile = DEFAULT_ORBIT_CONFIG_LOCATION;
                gsFile = DEFAULT_GS_CONFIG_LOCATION;
                orekitData = new File(DEFAULT_CONFIG_FOLDER + File.separator + OREKIT_FOLDER_NAME);
            }
            // Orekit initialisation
            try {
                DataProvidersManager orekitManager = DataContext.getDefault().getDataProvidersManager();
                orekitManager.addProvider(new DirectoryCrawler(orekitData));
            } catch (Exception e) {
                // You have to quit
                System.err.println("Orekit initialisation data not found. Steps to fix the problem:\n" +
                        "1) download https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip\n" +
                        "2) extract the archive and rename the resulting extracted folder to 'orekit-data'\n" +
                        "3) either copy the 'orekit-data' folder\n" +
                        "\t3a) inside " + DEFAULT_CONFIG_FOLDER + " or \n" +
                        "\t3b) inside another folder of your choice and " +
                        "start Dr. Orbiteex JVM with the system property -D" + CONFIG_FOLDER_LOCATION_KEY + "=<path to your folder>");
                e.printStackTrace();
                System.exit(-1);
            }
            // Load the model manager
            ModelManager manager = new ModelManager(orbitFile, gsFile);

            Platform.runLater(() -> {
                // Then the rest
                {
                    Parent root;
                    FXMLLoader loader = new FXMLLoader(DrOrbiteex.class.getResource("/eu/dariolucia/drorbiteex/fxml/Main.fxml"));
                    Main controller;
                    try {
                        root = loader.load();
                        controller = loader.getController();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

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
                    controller.configure(manager, () -> {
                        stage.show();
                        // Still wait 3 seconds in another thread (time to do some calculation) and then close the splash
                        waitAndClose(substage);
                    });
                }
            });
        }).start();
    }

    private void waitAndClose(Stage substage) {
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                // Do nothing... yes, yes, wrong, whatever...
            }
            Platform.runLater(substage::close);
        }).start();
    }

    public static void shutdown() {
        if (DialogUtils.confirm("Exit " + APPLICATION_NAME, "Exit " + APPLICATION_NAME, "Do you want to close " + APPLICATION_NAME + "?")) {
            Platform.exit();
            System.exit(0);
        }
    }
}
