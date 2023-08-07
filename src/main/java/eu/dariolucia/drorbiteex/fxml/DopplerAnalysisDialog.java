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

import eu.dariolucia.drorbiteex.model.collinearity.DopplerAnalysisRequest;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class DopplerAnalysisDialog implements Initializable {
    private final static Map<String, Double> lastUsedFrequency = new HashMap<>();
    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField frequencyText;
    private String error;

    private Dialog<?> dialog;
    private VisibilityWindow visibilityWindow;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ChangeListener<Object> validationBroker =  (prop, oldVal, newVal) -> validate();
        frequencyText.textProperty().addListener(validationBroker);
        validate();
    }

    private void validate() {
        try {
            if(!frequencyText.getText().isBlank()) {
                Double.parseDouble(frequencyText.getText());
            }
            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public DopplerAnalysisRequest getResult() {
        try {
            double frequency = 0;
            if(!frequencyText.getText().isBlank()) {
                frequency = Double.parseDouble(frequencyText.getText());
            }
            lastUsedFrequency.put(visibilityWindow.getOrbit().getName(), frequency);
            return new DopplerAnalysisRequest(visibilityWindow, frequency);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DopplerAnalysisRequest openDialog(Window owner, VisibilityWindow visibilityWindow) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Show Doppler for " + visibilityWindow.getOrbit().getName() + " on " + visibilityWindow.getStation().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = DopplerAnalysisDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/DopplerAnalysisDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            DopplerAnalysisDialog controller = loader.getController();
            controller.initialise(d, visibilityWindow);

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

    private void initialise(Dialog<?> d, VisibilityWindow visibilityWindow) {
        this.dialog = d;
        this.visibilityWindow = visibilityWindow;
        Double lastFrequency = lastUsedFrequency.get(visibilityWindow.getOrbit().getName());
        if(lastFrequency != null) {
            this.frequencyText.setText(String.valueOf(lastFrequency.doubleValue()));
        }
        validate();
    }
}
