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

import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalyser;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalysisRequest;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityEvent;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class CollinearityReportDialog implements Initializable {

    private static String lastExportFolder = null;

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public Label titleLabel;
    public TextField filterText;
    public TableView<CollinearityEvent> table;
    public PolarPlot polarPlotController;
    public Label referenceElAzLabel;
    public Label targetElAzLabel;
    public Label angularSeparationLabel;
    public Label nbEventsLabel;
    public VBox polarPlotParent;

    private CollinearityAnalysisRequest request;
    private FilteredList<CollinearityEvent> filteredList;
    private SortedList<CollinearityEvent> sortedList;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        polarPlotController.setSpacecraftDrawStrategy(this::drawSpacecraft);
        polarPlotController.setBackgroundColor(Color.valueOf("#101010"));
        polarPlotController.setForegroundColor(Color.WHITE);
        Platform.runLater(this::updatePolarPlotSize);
        filterText.textProperty().addListener((w, o, n) -> applyFilter(n));
        table.getSelectionModel().selectedItemProperty().addListener((w,o,n) -> selectEvent(n));

        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(0)).setCellValueFactory(o -> new ReadOnlyStringWrapper(TimeUtils.formatDate(o.getValue().getTime())));
        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(1)).setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getTargetOrbit().getName()));
        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(2)).setCellValueFactory(o -> new ReadOnlyStringWrapper(formatDouble(o.getValue().getAngularSeparation(), 3)));
    }

    private void updatePolarPlotSize() {
        double size = Math.min(polarPlotParent.getWidth(), polarPlotParent.getHeight());
        size = Math.min(400, size);
        size = Math.max(200, size);
        polarPlotController.updateSize(size - 1);
    }

    private void drawSpacecraft(GraphicsContext gc, Color color, Point2D p1, String orbitName) {
        if(orbitName.equals(request.getReferenceOrbit().getName())) {
            // Reference orbit
            gc.setLineWidth(1.5);
            gc.setFill(Color.WHITE);
            gc.fillOval(p1.getX() - 5, p1.getY() - 5, 10, 10);
            gc.setStroke(Color.LIMEGREEN);
            gc.strokeOval(p1.getX() - 5, p1.getY() - 5, 10, 10);
            gc.setFill(color);
            gc.setStroke(color);
        } else {
            // Target orbit
            gc.fillOval(p1.getX() - 3, p1.getY() - 3, 6, 6);
        }
    }

    private void selectEvent(CollinearityEvent event) {
        polarPlotController.clear();
        if(event == null) {
            referenceElAzLabel.setText("---");
            targetElAzLabel.setText("---");
            angularSeparationLabel.setText("---");
        } else {
            referenceElAzLabel.setText(formatElAz(event.getReferencePoint()));
            targetElAzLabel.setText(formatElAz(event.getTargetPoint()));
            angularSeparationLabel.setText(formatDouble(event.getAngularSeparation(), 3));

            polarPlotController.setSpacecraftPosition(event.getReferenceOrbit().getId(), event.getReferenceOrbit().getName(), toPoint(event.getReferencePoint()), Color.valueOf(event.getReferenceOrbit().getColor()));
            polarPlotController.setSpacecraftPosition(event.getTargetOrbit().getId(), event.getTargetOrbit().getName(), toPoint(event.getTargetPoint()), Color.valueOf(event.getTargetOrbit().getColor()));
        }
    }

    private Point2D toPoint(TrackPoint currentLocation) {
        return new Point2D(currentLocation.getAzimuth(), currentLocation.getElevation());
    }

    private String formatDouble(double value, int places) {
        return String.format("%." + places + "f", value);
    }

    private String formatElAz(TrackPoint referencePoint) {
        return formatDouble(referencePoint.getElevation(),2) + " | " + formatDouble(referencePoint.getAzimuth(), 2);
    }

    private void applyFilter(String orbitText) {
        if(orbitText == null || orbitText.isBlank()) {
            filteredList.setPredicate(e -> true);
        } else {
            filteredList.setPredicate(e -> e.getTargetOrbit().getName().toLowerCase().contains(orbitText.toLowerCase()));
        }
        nbEventsLabel.setText(String.valueOf(filteredList.size()));
    }

    public static void openDialog(Window owner, CollinearityAnalysisRequest request, List<CollinearityEvent> events) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Collinearity results of " + request.getReferenceOrbit().getName() + " for " + request.getGroundStation().getName() + " - Period: " +
                    TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()));
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = CollinearityReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/CollinearityReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            CollinearityReportDialog controller = loader.getController();
            controller.configure(request, events);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configure(CollinearityAnalysisRequest request, List<CollinearityEvent> events) {
        this.request = request;
        this.filteredList = new FilteredList<>(FXCollections.observableList(events), e -> true);
        this.sortedList = new SortedList<>(this.filteredList);
        this.table.setItems(this.sortedList);
        this.sortedList.comparatorProperty().bind(this.table.comparatorProperty());
        this.titleLabel.setText("Ground Station: " + request.getGroundStation().getName() + " - Reference Orbit: " + request.getReferenceOrbit().getName() + " - Period: "
            + TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()));
        this.nbEventsLabel.setText(String.valueOf(this.filteredList.size()));
    }

    public void onExportButtonAction(ActionEvent actionEvent) {
        // open file dialog
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CSV collinearity events for " + request.getGroundStation().getName());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV file","*.csv", "*.txt"));
        if(lastExportFolder != null) {
            fc.setInitialDirectory(new File(lastExportFolder));
        }
        fc.setInitialFileName("CollinearityEvents_" + request.getGroundStation().getCode() + "_" + request.getReferenceOrbit().getName() + "_"
                + TimeUtils.formatDate(request.getStartTime()).replace(" ","_").replace(".","").replace(":","") + "_"
                + TimeUtils.formatDate(request.getEndTime()).replace(" ","_").replace(".","").replace(":","")
                + ".csv" );
        File selected = fc.showSaveDialog(titleLabel.getScene().getWindow());
        if(selected != null) {
            lastExportFolder = selected.getParentFile().getAbsolutePath();
            // Export
            try {
                CollinearityAnalyser.generateCSV(selected.getAbsolutePath(), this.table.getItems());
                Platform.runLater(() -> DialogUtils.info("CSV collinearity events", "Collinearity events of " + request.getGroundStation().getName() + " exported", "File: " + selected.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> DialogUtils.alert("CSV collinearity events", "Collinearity events of " + request.getGroundStation().getName() + " not exported", "I/O Error: " + e.getMessage()));
            }
        }
    }
}
