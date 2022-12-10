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
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.schedule.ScheduleGenerationRequest;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.GroundStationParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;
import java.util.function.Supplier;


public class GroundStationPane implements Initializable {

    public ListView<GroundStationGraphics> groundStationList;

    // Pass table
    public TableView<VisibilityWindow> passTable;
    public TableColumn<VisibilityWindow, String> satelliteColumn;
    public TableColumn<VisibilityWindow, String> orbitColumn;
    public TableColumn<VisibilityWindow, String> aosColumn;
    public TableColumn<VisibilityWindow, String> losColumn;

    // Polar plot
    public PolarPlot polarPlotController;
    public ProgressIndicator polarPlotProgress;

    private ModelManager manager;
    private Supplier<List<Orbit>> orbitSupplier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        groundStationList.setCellFactory(CheckBoxListCell.forListView(GroundStationGraphics::visibleProperty));

        // Polar plot color configuration
        polarPlotController.setForegroundColor(Color.LIMEGREEN);
        polarPlotController.setBackgroundColor(Color.BLACK);

        // Configure pass table
        satelliteColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getOrbit().getName()));
        orbitColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(String.valueOf(o.getValue().getOrbitNumber())));
        aosColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getAosString()));
        losColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getLosString()));
        groundStationList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> refreshPassTableSelection(b));
        groundStationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        passTable.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> updatePolarPlotSelection(c));
    }

    public void configure(ModelManager manager, Supplier<List<Orbit>> orbitSupplier) {
        this.manager = manager;
        this.orbitSupplier = orbitSupplier;
    }

    private void refreshPassTableSelection(GroundStationGraphics b) {
        // If there is already a pass selected, remember it
        VisibilityWindow selected = passTable.getSelectionModel().getSelectedItem();
        passTable.getItems().clear();
        if(b != null) {
            Map<Orbit, List<VisibilityWindow>> windows = b.getGroundStation().getAllVisibilityWindows();
            List<VisibilityWindow> vw = new LinkedList<>();
            windows.values().forEach(vw::addAll);
            Collections.sort(vw);
            passTable.getItems().addAll(vw);
            // If there was a selection, re-select it
            if(selected != null && selected.getStation().equals(b.getGroundStation())) {
                // Look for the visibility and select it
                for(VisibilityWindow window : passTable.getItems()) {
                    if(window.getOrbit().equals(selected.getOrbit()) && window.getOrbitNumber() == selected.getOrbitNumber()) {
                        // Found
                        passTable.getSelectionModel().select(window);
                        break;
                    }
                }
            }
        }
    }

    private void updatePolarPlotSelection(VisibilityWindow c) {
        if(c == null) {
            this.polarPlotController.clear();
        } else {
            this.polarPlotController.setSpacecraftTrack(c);
            Color trackColor = Color.valueOf(c.getOrbit().getColor());
            this.polarPlotController.setTrackColor(trackColor);
            this.polarPlotController.setSpacecraftColor(trackColor);
            // Check if the spacecraft is in visibility
            SpacecraftPosition sp = c.getOrbit().getCurrentSpacecraftPosition();
            if(sp != null) {
                TrackPoint tp = c.getStation().getTrackPointOf(sp);
                if(tp != null && c.isInPass(tp.getTime())) {
                    this.polarPlotController.setNewSpacecraftPosition(c.getStation(), c.getOrbit(), tp);
                }
            }
        }
    }

    public void onNewGroundStationAction(ActionEvent actionEvent) {
        GroundStation gs = GroundStationDialog.openDialog(groundStationList.getParent().getScene().getWindow());
        // Register the ground station to the manager: the callbacks will do the rest
        if(gs != null) {
            BackgroundThread.runLater(() -> manager.getGroundStationManager().newGroundStation(
                    gs.getCode(), gs.getName(), gs.getSite(), gs.getDescription(), gs.getColor(), gs.isVisible(), gs.getLatitude(), gs.getLongitude(), gs.getHeight()
            ));
        }
    }

    public void onEditGroundStationAction(ActionEvent mouseEvent) {
        editGroundStation();
    }

    public void onDeleteGroundStationAction(ActionEvent actionEvent) {
        List<GroundStationGraphics> gsGraphics = groundStationList.getSelectionModel().getSelectedItems();
        if(gsGraphics != null && !gsGraphics.isEmpty()) {
            boolean confirmed = false;
            if(gsGraphics.size() == 1) {
                // One gs
                confirmed = DialogUtils.confirm("Delete Ground Station", null, "Do you want to delete ground station " + gsGraphics.get(0).getName() + "?");
            } else {
                // Multiple gs
                confirmed = DialogUtils.confirm("Delete Ground Station", null, "Do you want to delete the selected ground stations?");
            }
            if (confirmed) {
                BackgroundThread.runLater(() ->  {
                    for(GroundStationGraphics og : gsGraphics) {
                        manager.getGroundStationManager().removeGroundStation(og.getGroundStation().getId());
                    }
                });
            }
        }
    }

    public void onGroundStationSelectionClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            editGroundStation();
        }
    }

    private void editGroundStation() {
        GroundStationGraphics originalGs = groundStationList.getSelectionModel().getSelectedItem();
        if(originalGs != null) {
            GroundStation gs = GroundStationDialog.openDialog(groundStationList.getParent().getScene().getWindow(), originalGs.getGroundStation());
            if (gs != null) {
                // Go via the manager, callback will do the rest
                BackgroundThread.runLater(() -> originalGs.getGroundStation().update(gs));
            }
        }
    }

    public void onGenerateScheduleAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            List<Orbit> orbits = orbitSupplier.get(); //
            // open dialog
            ScheduleGenerationRequest sgr = ExportScheduleDialog.openDialog(groundStationList.getScene().getWindow(), gs.getGroundStation(), orbits);
            if(sgr != null) {
                BackgroundThread.runLater(() -> {
                    try {
                        final String finalPath = manager.exportSchedule(sgr);
                        Platform.runLater(() -> DialogUtils.info("CCSDS Simple Schedule Export", "Schedule of " + gs.getGroundStation().getName() + " exported", "Schedule file: " + finalPath));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("CCSDS Simple Schedule Export", "Schedule of " + gs.getGroundStation().getName() + " not exported", "Error: " + e.getMessage()));
                    }
                });
            }
        }
    }

    public void onSettingsGroundStationAction(ActionEvent actionEvent) {
        GroundStationParameterConfiguration originalProps = this.manager.getGroundStationManager().getConfiguration();
        GroundStationParameterConfiguration props = GroundStationConfigurationDialog.openDialog(groundStationList.getParent().getScene().getWindow(), originalProps);
        if(props != null) {
            BackgroundThread.runLater(() -> manager.updateGroundStationParameters(props)); // This triggers a full update
        }
    }

    public GroundStationGraphics registerNewGroundStation(GroundStation gs) {
        GroundStationGraphics graphics = new GroundStationGraphics(this.manager, gs);
        groundStationList.getItems().add(graphics);
        return graphics;
    }

    public Optional<GroundStationGraphics> getGraphicsOf(GroundStation gs) {
        return groundStationList.getItems().stream().filter(o -> o.getGroundStation().equals(gs)).findFirst();
    }

    public void deregisterGroundStation(GroundStationGraphics gs) {
        groundStationList.getItems().remove(gs);
        gs.dispose();
        groundStationList.refresh();
    }

    public void refreshPassTableSelection() {
        refreshPassTableSelection(groundStationList.getSelectionModel().getSelectedItem());
    }

    public List<GroundStationGraphics> getGroundStationGraphics() {
        return groundStationList.getItems();
    }

    public void refreshGroundStationList() {
        groundStationList.refresh();
    }

    public void refreshGroundStationOrbitData(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, TrackPoint currentPoint) {
        this.polarPlotController.updateCurrentData(groundStation, orbit, visibilityWindows);
        this.polarPlotController.setNewSpacecraftPosition(groundStation, orbit, currentPoint);
        if(groundStationList.getSelectionModel().getSelectedItem() != null && groundStation.equals(groundStationList.getSelectionModel().getSelectedItem().getGroundStation())) {
            // Force refresh of visibility windows
            refreshPassTableSelection();
        }
    }

    public void refreshSpacecraftPosition(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        polarPlotController.setNewSpacecraftPosition(groundStation, orbit, point);
    }


}
