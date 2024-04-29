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

import eu.dariolucia.drorbiteex.model.collinearity.OrbitVisibilityAnalysisRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class OrbitVisibilityAnalysisDialog implements Initializable {

    private static Date lastStartDate = new Date(); // now
    private static Date lastEndDate = new Date(lastStartDate.getTime() + (24 * 3600 * 1000)); // 1 day
    private static final List<String> lastSelectedGroundStationNames = new LinkedList<>();

    private static String lastExportFolder = null;

    public DatePicker startDatePicker;
    public TextField startTimeText;
    public DatePicker endDatePicker;
    public TextField endTimeText;

    public ListView<GroundStationWrapper> groundStationList;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField exportFolderText;
    private String error;

    private Orbit referenceOrbit;
    private Dialog<?> dialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        groundStationList.setCellFactory(CheckBoxListCell.forListView(GroundStationWrapper::selectedProperty));

        ChangeListener<Object> validationBroker =  (prop, oldVal, newVal) -> validate();

        startDatePicker.valueProperty().addListener(validationBroker);
        startTimeText.textProperty().addListener(validationBroker);
        endDatePicker.valueProperty().addListener(validationBroker);
        endTimeText.textProperty().addListener(validationBroker);
        exportFolderText.textProperty().addListener(validationBroker);

        validate();
    }

    private void validate() {
        try {
            if(startDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("Start date field is blank");
            }
            if(startTimeText.getText().isBlank()) {
                throw new IllegalStateException("Start time field is blank");
            }
            if(endDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("End date field is blank");
            }
            if(endTimeText.getText().isBlank()) {
                throw new IllegalStateException("End time field is blank");
            }

            DialogUtils.getDate(startDatePicker, startTimeText);
            DialogUtils.getDate(endDatePicker, endTimeText);

            File f = new File(exportFolderText.getText());
            if(!f.exists() || !f.isDirectory()) {
                throw new IllegalStateException("Export folder does not exist/is not a folder");
            }

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public OrbitVisibilityAnalysisRequest getResult() {
        try {
            Date start = DialogUtils.getDate(startDatePicker, startTimeText);
            Date end = DialogUtils.getDate(endDatePicker, endTimeText);
            lastStartDate = start;
            lastEndDate = end;
            List<GroundStation> groundStations = groundStationList.getItems().stream().filter(GroundStationWrapper::isSelected).map(GroundStationWrapper::getGroundStation).collect(Collectors.toList());
            lastSelectedGroundStationNames.clear();
            lastSelectedGroundStationNames.addAll(groundStations.stream().map(GroundStation::getName).collect(Collectors.toList()));

            return new OrbitVisibilityAnalysisRequest(start, end, referenceOrbit, groundStations, exportFolderText.getText());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static OrbitVisibilityAnalysisRequest openDialog(Window owner, Orbit gs, List<GroundStation> groundStations) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Export orbit visibility for " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OrbitVisibilityAnalysisDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/OrbitVisibilityAnalysisDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            OrbitVisibilityAnalysisDialog controller = loader.getController();
            controller.initialise(d, gs, groundStations);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
            ok.disableProperty().bind(controller.validData.not());
            Optional<ButtonType> result = d.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                return controller.getResult();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initialise(Dialog<?> d, Orbit gs, List<GroundStation> groundStations) {
        this.dialog = d;
        this.referenceOrbit = gs;
        this.startDatePicker.setValue(DialogUtils.toDateText(lastStartDate));
        this.startTimeText.setText(DialogUtils.toTimeText(lastStartDate));
        this.endDatePicker.setValue(DialogUtils.toDateText(lastEndDate));
        this.endTimeText.setText(DialogUtils.toTimeText(lastEndDate));
        this.exportFolderText.setText(lastExportFolder);

        for(GroundStation o : groundStations) {
            GroundStationWrapper ow = new GroundStationWrapper(o);
            if(lastSelectedGroundStationNames.contains(ow.getGroundStation().getName())) {
                ow.selectedProperty().set(true);
            }
            this.groundStationList.getItems().add(ow);
        }

        validate();
    }

    public void onExportFolderButton(ActionEvent actionEvent) {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Save visibility analysis to folder");
        if(lastExportFolder != null) {
            fc.setInitialDirectory(new File(lastExportFolder));
        }
        File selected = fc.showDialog(exportFolderText.getScene().getWindow());
        if(selected != null) {
            exportFolderText.setText(selected.getAbsolutePath());
            lastExportFolder = selected.getAbsolutePath();
        }
    }
}
