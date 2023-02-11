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

import eu.dariolucia.drorbiteex.fxml.progress.IMonitorableCallable;
import eu.dariolucia.drorbiteex.fxml.progress.ProgressDialog;
import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.collinearity.*;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.schedule.ScheduleGenerationRequest;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.GroundStationParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class GroundStationPane implements Initializable {

    public ListView<GroundStationGraphics> groundStationList;

    // Pass table
    public TableView<VisibilityWindow> passTable;
    public TableColumn<VisibilityWindow, String> satelliteColumn;
    public TableColumn<VisibilityWindow, String> orbitColumn;
    public TableColumn<VisibilityWindow, String> aosColumn;
    public TableColumn<VisibilityWindow, String> losColumn;

    // Polar plot for pass tracking
    public PolarPlot polarPlotController;
    public VBox polarPlotParent;
    public Button generateScheduleButton;
    public Button exportVisibilityButton;
    public Button exportGroundTrackButton;
    public Button collinearityAnalysisButton;
    public Button skyCoverageAnalysisButton;
    public Button editGroundStationButton;
    public Button deleteGroundStationButton;

    private ModelManager manager;
    private Supplier<List<Orbit>> orbitSupplier;

    // Exporters variables
    private String visibilityCsvFolder;
    private String trackingCsvFolder;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        groundStationList.setCellFactory(CheckBoxListCell.forListView(GroundStationGraphics::visibleProperty));

        // Polar plot color configuration
        polarPlotController.setForegroundColor(Color.LIMEGREEN);
        polarPlotController.setBackgroundColor(Color.valueOf("#101010"));
        polarPlotParent.widthProperty().addListener((a,b,c) -> updatePolarPlotSize());
        polarPlotParent.heightProperty().addListener((a,b,c) -> updatePolarPlotSize());
        // Configure pass table
        satelliteColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getOrbit().getName()));
        orbitColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(String.valueOf(o.getValue().getOrbitNumber())));
        aosColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getAosString()));
        losColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getLosString()));
        groundStationList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> refreshPassTableSelection(b));
        groundStationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        passTable.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> updatePolarPlotSelection(c));
        polarPlotController.setNameVisible(true);
        // Button enablement
        generateScheduleButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        collinearityAnalysisButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        skyCoverageAnalysisButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        editGroundStationButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        deleteGroundStationButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        exportVisibilityButton.disableProperty().bind(groundStationList.getSelectionModel().selectedItemProperty().isNull());
        exportGroundTrackButton.disableProperty().bind(passTable.getSelectionModel().selectedItemProperty().isNull());

    }

    private void updatePolarPlotSize() {
        double size = Math.min(polarPlotParent.getWidth(), polarPlotParent.getHeight() - 24); // 24: height of the Pass Plot label
        polarPlotController.updateSize(size);
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

    // This method is called when a visibility window is selected, therefore you have to re-initialise the polar plot
    private void updatePolarPlotSelection(VisibilityWindow c) {
        this.polarPlotController.clear();
        if(c != null) {
            this.polarPlotController.setSpacecraftTrack(c);
            updatePolarPlotText(c);
            Color trackColor = Color.valueOf(c.getOrbit().getColor());
            this.polarPlotController.setColor(c.getOrbit().getId(), trackColor);
            // Check if the spacecraft is in visibility
            SpacecraftPosition sp = c.getOrbit().getCurrentSpacecraftPosition();
            if (sp != null) {
                TrackPoint tp = c.getStation().getTrackPointOf(sp);
                if (tp != null && c.isInPass(tp.getTime())) {
                    this.polarPlotController.setNewSpacecraftPosition(c.getStation(), c.getOrbit(), tp);
                    this.polarPlotController.setText(PolarPlot.PlotPosition.TOP_LEFT, c.getOrbit().getName() + "\nAZ " + doublePrint(tp.getAzimuth(), 4) + "\nEL " + doublePrint(tp.getElevation(), 4), this.polarPlotController.getForegroundColor());
                } else {
                    this.polarPlotController.setText(PolarPlot.PlotPosition.TOP_LEFT, c.getOrbit().getName(), this.polarPlotController.getForegroundColor());
                }
            } else {
                this.polarPlotController.setText(PolarPlot.PlotPosition.TOP_LEFT, c.getOrbit().getName(), this.polarPlotController.getForegroundColor());
            }
        }
        updatePolarPlotSize();
    }

    private String doublePrint(double value, int places) {
        return String.format("%." + places + "f", value);
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
                IMonitorableCallable<String> task = monitor -> {
                    ITaskProgressMonitor monitorBridge = new ITaskProgressMonitor() {
                        @Override
                        public void progress(long current, long total, String message) {
                            monitor.progress("CCSDS Simple Schedule Export", current, total, message);
                        }

                        @Override
                        public boolean isCancelled() {
                            return monitor.isCancelled();
                        }
                    };

                    try {
                        return manager.exportSchedule(sgr, monitorBridge);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                };
                ProgressDialog.Result<String> taskResult = ProgressDialog.openProgress(groundStationList.getScene().getWindow(), "CCSDS Simple Schedule Export", task, BackgroundThread.getExecutor());
                if(taskResult.getStatus() == ProgressDialog.TaskStatus.COMPLETED) {
                    DialogUtils.info("CCSDS Simple Schedule Export", "Schedule of " + gs.getGroundStation().getName() + " exported", "Schedule file: " + taskResult.getResult());
                } else if(taskResult.getStatus() == ProgressDialog.TaskStatus.CANCELLED) {
                    DialogUtils.alert("CCSDS Simple Schedule Export", "Schedule of " + gs.getGroundStation().getName() + " not exported", "Task cancelled by user");
                } else {
                    DialogUtils.alert("CCSDS Simple Schedule Export", "Schedule of " + gs.getGroundStation().getName() + " not exported", "Error during file generation");
                }
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
        VisibilityWindow currentVisibilityWindow = this.polarPlotController.updateCurrentData(groundStation, orbit, visibilityWindows);
        if(currentVisibilityWindow != null) {
            this.polarPlotController.setNewSpacecraftPosition(groundStation, orbit, currentPoint);
            updatePolarPlotText(currentVisibilityWindow);
        }
        if(groundStationList.getSelectionModel().getSelectedItem() != null && groundStation.equals(groundStationList.getSelectionModel().getSelectedItem().getGroundStation())) {
            // Force refresh of visibility windows
            refreshPassTableSelection();
        }
    }

    private void updatePolarPlotText(VisibilityWindow currentVisibilityWindow) {
        if(currentVisibilityWindow != null) {
            // BL: entry
            this.polarPlotController.setText(PolarPlot.PlotPosition.BOTTOM_LEFT, "Start\nAZ " + doublePrint(currentVisibilityWindow.getGroundTrack().get(0).getAzimuth(), 4) + "\n" + TimeUtils.formatDate(currentVisibilityWindow.getGroundTrack().get(0).getTime()), this.polarPlotController.getForegroundColor());
            // BR: exit
            this.polarPlotController.setText(PolarPlot.PlotPosition.BOTTOM_RIGHT,"End\nAZ " + doublePrint(currentVisibilityWindow.getGroundTrack().get(currentVisibilityWindow.getGroundTrack().size() - 1).getAzimuth(), 4) + "\n" + TimeUtils.formatDate(currentVisibilityWindow.getGroundTrack().get(currentVisibilityWindow.getGroundTrack().size() - 1).getTime()), this.polarPlotController.getForegroundColor());
            // TR: max elevation
            TrackPoint maxElPoint = currentVisibilityWindow.getMaxElevationPoint();
            if (maxElPoint != null) {
                this.polarPlotController.setText(PolarPlot.PlotPosition.TOP_RIGHT, "Max EL\nAZ " + doublePrint(maxElPoint.getAzimuth(), 4) + "\nEL " + doublePrint(maxElPoint.getElevation(), 4) + "\n" + TimeUtils.formatDate(maxElPoint.getTime()), this.polarPlotController.getForegroundColor());
            }
        }
    }

    public void refreshSpacecraftPosition(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        this.polarPlotController.setNewSpacecraftPosition(groundStation, orbit, point);
    }

    public void addSelectionSubscriber(Consumer<GroundStationGraphics> selectionListener) {
        this.groundStationList.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> selectionListener.accept(c));
    }

    public void onExportVisibilityWindowsAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            List<Orbit> orbits = orbitSupplier.get();
            // filter only currently visible orbits
            List<UUID> filteredOrbits = orbits.stream().filter(Orbit::isVisible).map(Orbit::getId).collect(Collectors.toList());
            // open file dialog
            FileChooser fc = new FileChooser();
            fc.setTitle("Save CSV visibility windows for ground station " + gs.getGroundStation().getCode());
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
            fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
            if(this.visibilityCsvFolder != null) {
                fc.setInitialDirectory(new File(this.visibilityCsvFolder));
            }
            fc.setInitialFileName("VisibilityWindows_" + gs.getGroundStation().getCode() + "_"
                    + TimeUtils.formatDate(new Date()).replace(" ","_").replace(".","").replace(":","")
                    + ".csv" );
            File selected = fc.showSaveDialog(this.groundStationList.getScene().getWindow());
            if(selected != null) {
                this.visibilityCsvFolder = selected.getParentFile().getAbsolutePath();
                // Export
                UUID gsId = gs.getGroundStation().getId();
                BackgroundThread.runLater(() -> {
                    try {
                        if (!selected.exists()) {
                            selected.createNewFile();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("CSV visibility windows", "Visibility windows of " + gs.getGroundStation().getName() + " not exported", "Cannot create file: " + e.getMessage()));
                        return;
                    }
                    try(FileOutputStream fs = new FileOutputStream(selected)) {
                        manager.getGroundStationManager().exportVisibilityPasses(gsId, fs, filteredOrbits);
                        Platform.runLater(() -> DialogUtils.info("CSV visibility windows", "Visibility windows of " + gs.getGroundStation().getName() + " exported", "File: " + selected.getAbsolutePath()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("CSV visibility windows", "Visibility windows of " + gs.getGroundStation().getName() + " not exported", "I/O Error: " + e.getMessage()));
                    }
                });
            }
        }
    }

    public void onCollinearityAnalysisAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            List<Orbit> orbits = orbitSupplier.get(); //
            // open dialog
            CollinearityAnalysisRequest sgr = CollinearityAnalysisDialog.openDialog(groundStationList.getScene().getWindow(), gs.getGroundStation(), orbits);
            if(sgr != null) {
                IMonitorableCallable<List<CollinearityEvent>> task = monitor -> {
                    ITaskProgressMonitor monitorBridge = new ITaskProgressMonitor() {
                        @Override
                        public void progress(long current, long total, String message) {
                            monitor.progress("Collinearity Analysis", current, total, message);
                        }

                        @Override
                        public boolean isCancelled() {
                            return monitor.isCancelled();
                        }
                    };
                    try {
                        return CollinearityAnalyser.analyse(sgr, monitorBridge);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                };
                ProgressDialog.Result<List<CollinearityEvent>> taskResult = ProgressDialog.openProgress(groundStationList.getScene().getWindow(), "Collinearity Analysis", task);
                if(taskResult.getStatus() == ProgressDialog.TaskStatus.COMPLETED) {
                    CollinearityReportDialog.openDialog(groundStationList.getScene().getWindow(), sgr, taskResult.getResult());
                } else if(taskResult.getStatus() == ProgressDialog.TaskStatus.CANCELLED) {
                    DialogUtils.alert("Collinearity Analysis", "Collinearity events for " + gs.getGroundStation().getName() + " with " + sgr.getReferenceOrbit().getName(),
                            "Task cancelled by user");
                } else {
                    DialogUtils.alert("Collinearity Analysis", "Collinearity events for " + gs.getGroundStation().getName() + " with " + sgr.getReferenceOrbit().getName(),
                            "Error: " + taskResult.getError().getMessage());
                }
            }
        }
    }

    public void onExportGroundTrackAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = this.groundStationList.getSelectionModel().getSelectedItem();
        VisibilityWindow vw = this.passTable.getSelectionModel().getSelectedItem();
        if(gs != null && vw != null) {
            // open file dialog
            FileChooser fc = new FileChooser();
            fc.setTitle("Save CSV ground track for ground station " + gs.getGroundStation().getCode() + " - Orbit: " + vw.getOrbit() + " - AOS: " + vw.getAosString());
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
            fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
            fc.setInitialFileName("GroundTrack_" + gs.getGroundStation().getCode() + "_" + vw.getOrbit().getName() +
                    (vw.getAos() == null ? "" : "_AOS_" + vw.getAosString().replace(" ","_").replace(".","").replace(":",""))
                    + ".csv" );
            if(this.trackingCsvFolder != null) {
                fc.setInitialDirectory(new File(this.trackingCsvFolder));
            }
            File selected = fc.showSaveDialog(this.groundStationList.getScene().getWindow());
            if(selected != null) {
                this.trackingCsvFolder = selected.getParentFile().getAbsolutePath();

                // Export
                UUID gsId = gs.getGroundStation().getId();
                UUID vwId = vw.getId();
                UUID orbitId = vw.getOrbit().getId();
                BackgroundThread.runLater(() -> {
                    try {
                        if (!selected.exists()) {
                            selected.createNewFile();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("CSV ground track", "Ground track of " + gs.getGroundStation().getName() + " not exported", "Cannot create file: " + e.getMessage()));
                        return;
                    }
                    try(FileOutputStream fs = new FileOutputStream(selected)) {
                        manager.getGroundStationManager().exportTrackingInfo(gsId, fs, orbitId, vwId);
                        Platform.runLater(() -> DialogUtils.info("CSV ground track", "Ground track of " + gs.getGroundStation().getName() + " exported", "File: " + selected.getAbsolutePath()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("CSV ground track", "Ground track of " + gs.getGroundStation().getName() + " not exported", "I/O Error: " + e.getMessage()));
                    }
                });
            }
        }
    }

    public void orbitUpdated(Orbit orbit) {
        for(GroundStationGraphics g : getGroundStationGraphics()) {
            g.informOrbitUpdated(orbit);
        }
    }

    public void deregisterOrbit(OrbitGraphics graphics) {
        for(GroundStationGraphics g : getGroundStationGraphics()) {
            g.informOrbitRemoved(graphics.getOrbit());
        }
    }

    public void onSkyCoverageAnalysisAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            List<Orbit> orbits = orbitSupplier.get();
            // open dialog
            SkyCoverageAnalysisRequest sgr = SkyCoverageAnalysisDialog.openDialog(groundStationList.getScene().getWindow(), gs.getGroundStation(), orbits);
            if(sgr != null) {
                IMonitorableCallable<Canvas> task = monitor -> {
                    ITaskProgressMonitor monitorBridge = new ITaskProgressMonitor() {
                        @Override
                        public void progress(long current, long total, String message) {
                            monitor.progress("Sky Coverage Analysis", current, total, message);
                        }

                        @Override
                        public boolean isCancelled() {
                            return monitor.isCancelled();
                        }
                    };
                    try {
                        return SkyCoverageAnalyser.analyse(sgr, monitorBridge);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                };
                ProgressDialog.Result<Canvas> taskResult = ProgressDialog.openProgress(groundStationList.getScene().getWindow(), "Sky Coverage Analysis", task);
                if(taskResult.getStatus() == ProgressDialog.TaskStatus.COMPLETED) {
                    SkyCoverageReportDialog.openDialog(groundStationList.getScene().getWindow(), sgr, taskResult.getResult());
                } else if(taskResult.getStatus() == ProgressDialog.TaskStatus.CANCELLED) {
                    DialogUtils.alert("Sky Coverage Analysis", "Sky coverage for " + gs.getGroundStation().getName(),
                            "Task cancelled by user");
                } else {
                    DialogUtils.alert("Sky Coverage Analysis", "Sky coverage for " + gs.getGroundStation().getName(),
                            "Error: " + taskResult.getError().getMessage());
                }
            }
        }
    }
}
