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

import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalysisRequest;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityEvent;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CollinearityReportDialog implements Initializable {

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public Label titleLabel;
    public TextField filterText;
    public Label timePeriodLabel;
    public TableView<CollinearityEvent> table;
    public PolarPlot polarPlotController;
    public Label referenceElAzLabel;
    public Label targetElAzLabel;
    public Label angularSeparationLabel;
    public Label nbEventsLabel;

    private CollinearityAnalysisRequest request;
    private FilteredList<CollinearityEvent> filteredList;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        polarPlotController.setForegroundColor(Color.LIMEGREEN);
        polarPlotController.setBackgroundColor(Color.BLACK);

        filterText.textProperty().addListener((w, o, n) -> applyFilter(n));
        table.getSelectionModel().selectedItemProperty().addListener((w,o,n) -> selectEvent(n));

        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(0)).setCellValueFactory(o -> new ReadOnlyStringWrapper(TimeUtils.formatDate(o.getValue().getTime())));
        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(1)).setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getTargetOrbit().getName()));
        ((TableColumn<CollinearityEvent, String>) table.getColumns().get(2)).setCellValueFactory(o -> new ReadOnlyStringWrapper(String.valueOf(o.getValue().getAngularSeparation())));
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
            CollinearityReportDialog controller = loader.getController();
            controller.initialise(request, events);

            d.getDialogPane().setContent(root);
            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialise(CollinearityAnalysisRequest request, List<CollinearityEvent> events) {
        this.request = request;
        this.filteredList = new FilteredList<>(FXCollections.observableList(events), e -> true);
        this.table.setItems(this.filteredList);
        this.titleLabel.setText("Ground Station: " + request.getGroundStation().getName() + " - Reference Orbit: " + request.getReferenceOrbit().getName() + " - Period: "
            + TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()));
        this.timePeriodLabel.setText(TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()));
        this.nbEventsLabel.setText(String.valueOf(this.filteredList.size()));
    }
}
