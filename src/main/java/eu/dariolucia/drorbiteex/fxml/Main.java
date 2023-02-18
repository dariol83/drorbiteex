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

import eu.dariolucia.drorbiteex.application.DrOrbiteex;
import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.IOrbitListener;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitManager;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main implements Initializable, IOrbitListener, IGroundStationListener {
    private static final String NO_GROUND_TRACK = "             ";
    public static final int UPDATE_PERIOD = 5000;

    // Ground Station Pane
    public GroundStationPane groundStationPaneController;

    // Orbit Pane
    public OrbitPane orbitPaneController;

    // 3D scene
    public Scene3D scene3dController;

    // 2D scene
    public Scene2D scene2dController;

    // Time tracker
    public ToggleButton timerRealTimeTrackingButton;
    public Label currentTimeLabel;
    private final Timer tracker = new Timer();
    public ToggleButton toggle3DvisibilityLineButton;
    public AnchorPane dropshadow;
    public AnchorPane polarPlotPane;
    public PolarPlot polarPlotController;
    public ToggleButton polarPlotButton;
    public Button stepBackwardTrackingButton;
    public Button stepForwardTrackingButton;
    public ToggleButton replayTrackingButton;
    public ToggleButton replay2SpeedTrackingButton;
    public ToggleButton replay4SpeedTrackingButton;
    public Button editReplayDateTimeButton;
    public Accordion accordion;

    private TimerTask timerTask = null;

    public ToggleButton minimapButton;
    public ToggleButton toggle3DviewButton;

    // Ground track combo selection
    public ComboBox<Object> groundTrackCombo;
    private ChangeListener<Boolean> visibilityUpdateListener = (observableValue, aBoolean, t1) -> update2Dscene();

    // Label to indicate processing on going
    public Label processingLabel;

    // Main scene parent node for 3D/2D views
    public VBox mainSceneParent;
    public StackPane mainSceneStackPane;
    public AnchorPane miniPane;
    public AnchorPane fullPane;

    private ModelManager manager;

    private boolean orbitUpdateInProgress = false;
    private Stage replayPanelEditStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Handle ground track list
        groundTrackCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Object o) {
                if (o.equals(NO_GROUND_TRACK)) {
                    return NO_GROUND_TRACK;
                } else if (o instanceof OrbitGraphics) {
                    return ((OrbitGraphics) o).getName();
                } else {
                    throw new IllegalStateException("Wrong conversion object: " + o);
                }
            }

            @Override
            public Object fromString(String s) {
                if (s.equals(NO_GROUND_TRACK)) {
                    return NO_GROUND_TRACK;
                } else {
                    for (OrbitGraphics ao : orbitPaneController.getOrbitGraphics()) {
                        if (ao.getName().equals(s)) {
                            return ao;
                        }
                    }
                    throw new IllegalStateException("Wrong conversion string: " + s);
                }
            }
        });
        groundTrackCombo.getItems().add(NO_GROUND_TRACK);
        orbitPaneController.getOrbitGraphics().addListener((ListChangeListener<OrbitGraphics>) c -> {
            while (c.next()) {
                for (OrbitGraphics remitem : c.getRemoved()) {
                    groundTrackCombo.getItems().remove(remitem);
                }
                for (OrbitGraphics additem : c.getAddedSubList()) {
                    groundTrackCombo.getItems().add(additem);
                }
            }
        });
        groundTrackCombo.getSelectionModel().select(0);

        // Bind visibility of minimap/polar plot to toggle button
        miniPane.getParent().visibleProperty().bind(this.minimapButton.selectedProperty());
        polarPlotPane.getParent().visibleProperty().bind(this.polarPlotButton.selectedProperty());

        // Configure 3D scene
        scene3dController.configure(fullPane);
        scene2dController.configure(miniPane);

        dropshadow.prefWidthProperty().bind(mainSceneParent.widthProperty());
        dropshadow.prefHeightProperty().bind(mainSceneParent.heightProperty());
        dropshadow.setBackground(new Background(
                new BackgroundImage(
                        new Image(DrOrbiteex.class.getResourceAsStream("/images/sky.png")),
                        BackgroundRepeat.REPEAT,
                        BackgroundRepeat.REPEAT,
                        BackgroundPosition.CENTER,
                        BackgroundSize.DEFAULT)
        ));

        polarPlotController.updateSize(300);
        polarPlotController.setNameVisible(true);
        polarPlotController.setBackgroundColor(Color.BLACK);
        polarPlotController.setForegroundColor(Color.LIMEGREEN);
        polarPlotController.refresh();

        groundStationPaneController.setGroundStationPolarPlot(polarPlotController);

        // Wiring button disabling depending on the status
        editReplayDateTimeButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replayTrackingButton.selectedProperty(), Bindings.or(replay2SpeedTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty())))
        );
        stepForwardTrackingButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replayTrackingButton.selectedProperty(), Bindings.or(replay2SpeedTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty())))
        );
        stepBackwardTrackingButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replayTrackingButton.selectedProperty(), Bindings.or(replay2SpeedTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty())))
        );
        timerRealTimeTrackingButton.disableProperty().bind(
                Bindings.or(replayTrackingButton.selectedProperty(), Bindings.or(replay2SpeedTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty()))
        );
        replayTrackingButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replay2SpeedTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty()))
        );
        replay2SpeedTrackingButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replayTrackingButton.selectedProperty(),
                        replay4SpeedTrackingButton.selectedProperty()))
        );
        replay4SpeedTrackingButton.disableProperty().bind(
                Bindings.or(timerRealTimeTrackingButton.selectedProperty(), Bindings.or(replayTrackingButton.selectedProperty(),
                        replay2SpeedTrackingButton.selectedProperty()))
        );

    }

    private void handleOrbitTracking(OrbitGraphics o) {
        // Forward to 2D scene controller
        scene2dController.activateTracking(o);
        // Forward to 3D scene controller (only to enable/disable mouse drag)
        scene3dController.activateTracking(o);
    }

    private OrbitGraphics getSelectedOrbit() {
        Object selectedSc = groundTrackCombo.getSelectionModel().getSelectedItem();
        if(selectedSc.equals(NO_GROUND_TRACK)) {
            return null;
        } else {
            return (OrbitGraphics) selectedSc;
        }
    }

    private void registerNewGroundStation(GroundStation gs) {
        GroundStationGraphics graphics = groundStationPaneController.registerNewGroundStation(gs);
        scene3dController.registerNewGroundStation(graphics);
        graphics.visibleProperty().addListener(this.visibilityUpdateListener);
        graphics.visibilityLineProperty().bind(this.toggle3DvisibilityLineButton.selectedProperty());
        update2Dscene();
    }

    private void deregisterGroundStation(GroundStation gs) {
        Optional<GroundStationGraphics> first = groundStationPaneController.getGraphicsOf(gs);
        if(first.isPresent()) {
            GroundStationGraphics graphics = first.get();
            scene3dController.deregisterGroundStation(graphics);
            graphics.visibleProperty().removeListener(this.visibilityUpdateListener);
            // This call disposes the graphics item
            groundStationPaneController.deregisterGroundStation(graphics);
            update2Dscene();
        }
    }

    private void registerNewOrbit(Orbit o) {
        OrbitGraphics graphics = orbitPaneController.registerNewOrbit(o);
        scene3dController.registerNewOrbit(graphics);
        graphics.visibleProperty().addListener(this.visibilityUpdateListener);
        update2Dscene();
    }

    private void deregisterOrbit(Orbit orbit) {
        Optional<OrbitGraphics> first = orbitPaneController.getGraphicsOf(orbit);
        if(first.isPresent()) {
            OrbitGraphics graphics = first.get();
            scene3dController.deregisterOrbit(graphics);
            scene2dController.deregisterOrbit(graphics);
            graphics.visibleProperty().removeListener(this.visibilityUpdateListener);
            // This call disposes the graphics item
            orbitPaneController.deregisterOrbit(graphics);
            groundStationPaneController.deregisterOrbit(graphics);
            update2Dscene();
            groundStationPaneController.refreshPassTableSelection();
        }
    }

    private void update2Dscene() {
        scene2dController.refreshScene();
    }

    public void onActivateRealTimeTrackingAction(ActionEvent actionEvent) {
        handleTimerActivation(this.timerRealTimeTrackingButton, this::getRealTimeDate);
    }

    private void handleTimerActivation(ToggleButton toggleButton, Supplier<Date> dateSupplier) {
        if(toggleButton.isSelected() && this.timerTask == null) {
            activateTrackingTimer(dateSupplier, getUpdatePeriod());
        } else if(!toggleButton.isSelected() && this.timerTask != null){
            deactivateTrackingTimer();
        }
    }

    private void deactivateTrackingTimer() {
        if(this.timerTask != null) {
            this.timerTask.cancel();
            this.timerTask = null;
        }
    }

    private int getUpdatePeriod() {
        if(timerRealTimeTrackingButton.isSelected() || replayTrackingButton.isSelected()) {
            return UPDATE_PERIOD;
        } else if(replay2SpeedTrackingButton.isSelected()) {
            return UPDATE_PERIOD/2;
        } else if(replay4SpeedTrackingButton.isSelected()) {
            return UPDATE_PERIOD/4;
        } else {
            throw new IllegalStateException("Cannot derive update period");
        }
    }

    private void activateTrackingTimer(Supplier<Date> dateSupplier, int waitPeriod) {
        this.timerTask = new TimerTask() {
            @Override
            public void run() {
                updateOrbitTime(dateSupplier.get(), false);
            }
        };
        this.tracker.schedule(timerTask, 0, waitPeriod);
    }

    private Date getRealTimeDate() {
        return new Date();
    }

    private Date getReplayTimeDate() {
        Date d = (Date) this.currentTimeLabel.getUserData();
        if(d == null) {
            d = new Date();
            this.currentTimeLabel.setText(TimeUtils.formatDate(d));
            this.currentTimeLabel.setUserData(d);
        } else {
            d = new Date(d.getTime() + UPDATE_PERIOD);
            // The update of the label will be done on notification
        }
        return d;
    }

    private void refreshModel() {
        BackgroundThread.runLater(() -> manager.getOrbitManager().refresh());
    }

    private void updateOrbitTime(Date now, boolean forceUpdate) {
        BackgroundThread.runLater(() -> manager.getOrbitManager().updateOrbitTime(now, forceUpdate));
    }

    public void onGroundTrackComboSelected(ActionEvent actionEvent) {
        update2Dscene();
    }

    @Override
    public void orbitAdded(OrbitManager manager, Orbit orbit) {
        Platform.runLater(() -> registerNewOrbit(orbit));
    }

    @Override
    public void orbitRemoved(OrbitManager manager, Orbit orbit) {
        Platform.runLater(() -> deregisterOrbit(orbit));
    }

    @Override
    public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        Platform.runLater(() -> {
            // If in tracking mode, you have to inform the 3D scene about realigning
            scene3dController.updateIfTrackingOrbit(orbit, currentPosition);
            orbitPaneController.updateSpacecraftPosition(orbit, currentPosition);
            orbitPaneController.refreshOrbitList();
            // Check if the ground station objects in the 3D view must hide visibility connections
            groundStationPaneController.orbitUpdated(orbit);
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void startOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        Platform.runLater(() -> {
            processingLabel.setBackground(new Background(new BackgroundFill(Color.valueOf("green"), null, null)));
            processingLabel.setText("UPDATE");
            orbitUpdateInProgress = true;
        });
    }

    @Override
    public void endOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        Platform.runLater(() -> {
            orbitUpdateInProgress = false;
            processingLabel.setBackground(null);
            processingLabel.setText("IDLE");
            update2Dscene();
        });
    }

    @Override
    public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        Platform.runLater(() -> {
            if(currentPosition != null) {
                this.currentTimeLabel.setText(TimeUtils.formatDate(currentPosition.getTime()));
                this.currentTimeLabel.setUserData(currentPosition.getTime());
            }
            // If in tracking mode, you have to inform the 3D scene about realigning
            scene3dController.updateIfTrackingOrbit(orbit, currentPosition);
            orbitPaneController.updateSpacecraftPosition(orbit, currentPosition);
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void groundStationAdded(GroundStationManager manager, GroundStation groundStation) {
        Platform.runLater(() -> registerNewGroundStation(groundStation));
    }

    @Override
    public void groundStationRemoved(GroundStationManager manager, GroundStation groundStation) {
        Platform.runLater(() -> deregisterGroundStation(groundStation));
    }

    @Override
    public void groundStationUpdated(GroundStation groundStation) {
        Platform.runLater(() -> {
            groundStationPaneController.refreshGroundStationList();
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
        Platform.runLater(() -> {
            if(currentPoint != null) {
                this.currentTimeLabel.setText(TimeUtils.formatDate(currentPoint.getTime()));
                this.currentTimeLabel.setUserData(currentPoint.getTime());
            }
            groundStationPaneController.refreshGroundStationOrbitData(groundStation, orbit, visibilityWindows, currentPoint);
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        Platform.runLater(() -> {
            if(point != null) {
                this.currentTimeLabel.setText(TimeUtils.formatDate(point.getTime()));
                this.currentTimeLabel.setUserData(point.getTime());
            }
            groundStationPaneController.refreshSpacecraftPosition(groundStation, orbit, point);
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    public void onForceOrbitComputationAction(ActionEvent actionEvent) {
        refreshModel();
    }

    public List<Orbit> getOrbits() {
        return orbitPaneController.getOrbitGraphics().stream().map(OrbitGraphics::getOrbit).collect(Collectors.toList());
    }

    public void on3DviewButtonAction(ActionEvent actionEvent) {
        if(this.toggle3DviewButton.isSelected()) {
            make3DsceneLarger();
        } else {
            make2DsceneLarger();
        }
    }

    private void make2DsceneLarger() {
        // Get the 2D scene
        Node scene2Dmain = this.scene2dController.getMainScene();
        // Get the 3D scene
        Node scene3Dmain = this.scene3dController.getMainScene();
        // Remove the 3D scene from the fullPane
        fullPane.getChildren().remove(scene3Dmain);
        // Remove the 2D scene from the miniPane
        miniPane.getChildren().remove(scene2Dmain);
        // Swap
        fullPane.getChildren().add(scene2Dmain);
        miniPane.getChildren().add(scene3Dmain);
        // Reconfigure 3D scene
        this.scene3dController.setBackgroundColor(Color.BLACK);
        this.scene3dController.configure(miniPane);
        this.scene2dController.configure(fullPane);
        this.scene2dController.recomputeViewports(true);
    }

    private void make3DsceneLarger() {
        // Get the 2D scene
        Node scene2Dmain = this.scene2dController.getMainScene();
        // Get the 3D scene
        Node scene3Dmain = this.scene3dController.getMainScene();
        // Remove the 3D scene from the miniPane
        miniPane.getChildren().remove(scene3Dmain);
        // Remove the 2D scene from the fullPane
        fullPane.getChildren().remove(scene2Dmain);
        // Swap
        miniPane.getChildren().add(scene2Dmain);
        fullPane.getChildren().add(scene3Dmain);
        // Reconfigure 3D scene
        this.scene3dController.setBackgroundColor(Color.TRANSPARENT);
        this.scene3dController.configure(fullPane);
        this.scene2dController.configure(miniPane);
        this.scene2dController.recomputeViewports(true);
    }

    public void configure(ModelManager manager, Runnable onFinish) {
        this.manager = manager;
        // Register to the model manager
        this.manager.getOrbitManager().addListener(this);
        this.manager.getGroundStationManager().addListener(this);

        // Ground Station Pane configuration
        this.orbitPaneController.configure(this.manager, this::handleOrbitTracking);
        this.groundStationPaneController.configure(this.manager, this::getOrbits);
        this.scene2dController.setDataSuppliers(orbitPaneController::getOrbitGraphics, groundStationPaneController::getGroundStationGraphics, this::getSelectedOrbit);

        // Create graphics objects
        for(Orbit o : this.manager.getOrbitManager().getOrbits()) {
            registerNewOrbit(o);
        }

        for(GroundStation gs : this.manager.getGroundStationManager().getGroundStations()) {
            registerNewGroundStation(gs);
        }

        // Subscribe 2D scene to orbit pane for selection
        this.orbitPaneController.addSelectionSubscriber(this.scene2dController::setSelectedOrbit);
        this.groundStationPaneController.addSelectionSubscriber(this.scene2dController::setSelectedGroundStation);

        // Redraw stuff on the 2D scene
        update2Dscene();

        // Activate satellite tracking
        timerRealTimeTrackingButton.setSelected(true);
        onActivateRealTimeTrackingAction(null);

        // Expand first accordion panel
        accordion.setExpandedPane(accordion.getPanes().get(0));

        onFinish.run();
    }

    public void onAboutAction(ActionEvent actionEvent) {
        DialogUtils.info("About " + DrOrbiteex.APPLICATION_NAME + "...", DrOrbiteex.APPLICATION_NAME + " " + DrOrbiteex.VERSION,
                String.format("Orbit visualisation and processing application\n\nCopyright (c) 2022-2023 Dario Lucia\n\nhttps://www.dariolucia.eu\n" +
                        "https://github.com/dariol83/drorbiteex"));
    }

    public void onStepBackwardTrackingAction(ActionEvent actionEvent) {
        Date next = new Date(getReplayTimeDate().getTime() - 2*UPDATE_PERIOD);
        updateOrbitTime(next, false);
    }

    public void onStepForwardTrackingAction(ActionEvent actionEvent) {
        Date next = getReplayTimeDate();
        updateOrbitTime(next, false);
    }

    public void onActivateReplayTrackingAction(ActionEvent actionEvent) {
        handleTimerActivation(this.replayTrackingButton, this::getReplayTimeDate);
    }

    public void onActivateReplay2SpeedTrackingAction(ActionEvent actionEvent) {
        handleTimerActivation(this.replay2SpeedTrackingButton, this::getReplayTimeDate);
    }

    public void onActivateReplay4SpeedTrackingAction(ActionEvent actionEvent) {
        handleTimerActivation(this.replay4SpeedTrackingButton, this::getReplayTimeDate);
    }

    public void editReplayDateTimeAction(ActionEvent actionEvent) {
        if(this.replayPanelEditStage == null) {
            Bounds bounds = editReplayDateTimeButton.getBoundsInLocal();
            Bounds screenBounds = editReplayDateTimeButton.localToScreen(bounds);
            double x = screenBounds.getMinX();
            double y = screenBounds.getMinY();
            double height = screenBounds.getHeight();
            this.replayPanelEditStage = DateTimePickerPanel.openDialog(
                    editReplayDateTimeButton.getScene().getWindow(), getReplayTimeDate(),
                    o -> {
                        if(o != null){
                            updateOrbitTime(o, true);
                        }
                        this.replayPanelEditStage = null;
                    },
                    new Point2D(x, y + height));
        } else {
            this.replayPanelEditStage.close();
            this.replayPanelEditStage = null;
        }
    }
}
