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

import eu.dariolucia.drorbiteex.model.station.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class GroundStationDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextField siteText;
    public TextArea descriptionTextArea;
    public TextField latitudeText;
    public TextField longitudeText;
    public TextField altitudeText;
    public ColorPicker colorPicker;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        latitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        longitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        altitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
            }
            if(nameText.getText().isBlank()) {
                throw new IllegalStateException("Name field is blank");
            }
            Double.parseDouble(latitudeText.getText());
            Double.parseDouble(longitudeText.getText());
            Double.parseDouble(altitudeText.getText());
            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void setOriginalGroundStation(GroundStation gs) {
        codeText.setText(gs.getCode());
        nameText.setText(gs.getName());
        siteText.setText(gs.getSite());
        descriptionTextArea.setText(gs.getDescription());
        latitudeText.setText(String.valueOf(gs.getLatitude()));
        longitudeText.setText(String.valueOf(gs.getLongitude()));
        altitudeText.setText(String.valueOf(gs.getHeight()));
        colorPicker.setValue(Color.valueOf(gs.getColor()));
    }

    public GroundStation getResult() {
        return new GroundStation(UUID.randomUUID(), codeText.getText(), nameText.getText(), siteText.getText(), descriptionTextArea.getText(),colorPicker.getValue().toString(), true,
                Double.parseDouble(latitudeText.getText()), Double.parseDouble(longitudeText.getText()), Double.parseDouble(altitudeText.getText()));
    }

    public static GroundStation openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static GroundStation openDialog(Window owner, GroundStation gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Ground Station");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = GroundStationDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/GroundStationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            GroundStationDialog controller = loader.getController();
            if(gs != null) {
                controller.setOriginalGroundStation(gs);
            }

            d.getDialogPane().setContent(root);
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
}
