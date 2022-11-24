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

import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.GroundStationParameterConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class GroundStationConfigurationDialog implements Initializable {

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField elevationText;

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        elevationText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            Integer.parseInt(elevationText.getText());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void initialise(GroundStationParameterConfiguration p) {
        elevationText.setText(String.valueOf(p.getElevationThreshold()));

    }

    public GroundStationParameterConfiguration getResult() {
        return new GroundStationParameterConfiguration(Integer.parseInt(elevationText.getText()));
    }

    public static GroundStationParameterConfiguration openDialog(Window owner, GroundStationParameterConfiguration p) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Ground Station Parameters");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = GroundStationConfigurationDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/GroundStationConfigurationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            GroundStationConfigurationDialog controller = loader.getController();
            if(p != null) {
                controller.initialise(p);
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
