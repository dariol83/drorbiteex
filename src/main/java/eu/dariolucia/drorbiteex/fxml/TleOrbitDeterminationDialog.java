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

import eu.dariolucia.drorbiteex.model.determination.Measurement;
import eu.dariolucia.drorbiteex.model.determination.OemImporter;
import eu.dariolucia.drorbiteex.model.determination.TdmImporter;
import eu.dariolucia.drorbiteex.model.determination.TleOrbitDeterminationRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TleOrbitDeterminationDialog implements Initializable {

    private static Double lastUsedMass = null;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField massText;
    public TableView<Measurement> measurementTable;
    public TableColumn<Measurement, String> timeColumn;
    public TableColumn<Measurement, Measurement.Type> measureTypeColumn;
    public TableColumn<Measurement, String> measureInfoColumn;
    private String error;

    private Orbit referenceOrbit;
    private List<GroundStation> groundStations;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        timeColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(TimeUtils.formatDate(o.getValue().getTime())));
        measureTypeColumn.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getType()));
        measureInfoColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getInfo()));
        measurementTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ChangeListener<Object> validationBroker =  (prop, oldVal, newVal) -> validate();

        massText.textProperty().addListener(validationBroker);
        measurementTable.getItems().addListener((ListChangeListener<Measurement>) change -> validate());

        validate();
    }

    private void validate() {
        try {
            if(massText.getText().isBlank()) {
                throw new IllegalStateException("Mass field is blank");
            }
            if(measurementTable.getItems().isEmpty()) { // No measurements lead to exception
                throw new IllegalStateException("No measurements");
            }

            Double.parseDouble(massText.getText());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public TleOrbitDeterminationRequest getResult() {
        try {
            lastUsedMass = Double.parseDouble(massText.getText());

            return new TleOrbitDeterminationRequest(referenceOrbit, lastUsedMass, List.copyOf(measurementTable.getItems()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TleOrbitDeterminationRequest openDialog(Window owner, Orbit orbit, List<GroundStation> groundStations) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Orbit determination for " + orbit.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TleOrbitDeterminationDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/TleOrbitDeterminationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            TleOrbitDeterminationDialog controller = loader.getController();
            controller.initialise(orbit, groundStations);

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

    private void initialise(Orbit gs, List<GroundStation> groundStations) {
        this.referenceOrbit = gs;
        this.groundStations = groundStations;
        // set latest used values
        if(lastUsedMass != null) {
            this.massText.setText(String.valueOf(lastUsedMass));
        }
        validate();
    }

    public void onTdmLoadAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select TDM File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TDM file","*.xml", "*.tdm", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("TDM file","*.xml", "*.tdm", "*.txt"));

        File selected = fc.showOpenDialog(measurementTable.getScene().getWindow());
        if(selected != null) {
            // Read contents
            BackgroundThread.runLater(() -> {
                try {
                    List<Measurement> measurements = TdmImporter.load(selected.getAbsolutePath(), referenceOrbit, groundStations);
                    Platform.runLater(() -> {
                        measurementTable.getItems().addAll(measurements);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        DialogUtils.alert("TDM import failed", "Cannot read TDM file", e.getMessage());
                    });
                }
            });
        }
    }

    public void onDeleteAction(ActionEvent actionEvent) {
        List<Measurement> measToDelete = new ArrayList<>(this.measurementTable.getSelectionModel().getSelectedItems());
        this.measurementTable.getItems().removeAll(measToDelete);
        this.measurementTable.refresh();
    }

    public void onOemLoadAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select OEM File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));

        File selected = fc.showOpenDialog(measurementTable.getScene().getWindow());
        if(selected != null) {
            // Read contents
            BackgroundThread.runLater(() -> {
                try {
                    List<Measurement> measurements = OemImporter.load(selected.getAbsolutePath(), referenceOrbit);
                    Platform.runLater(() -> {
                        measurementTable.getItems().addAll(measurements);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        DialogUtils.alert("OEM import failed", "Cannot read OEM file", e.getMessage());
                    });
                }
            });
        }
    }
}
