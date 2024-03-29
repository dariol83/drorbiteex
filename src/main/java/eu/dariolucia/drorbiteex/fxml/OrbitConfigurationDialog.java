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

import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class OrbitConfigurationDialog implements Initializable {

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField beforePropagationStepsText;
    public TextField afterPropagationStepsText;
    public TextField propagationStepPeriodText;
    public TextField recomputeFullDataIntervalText;
    public ImageView warningImage;
    public Label warningLabel;

    private int nbOrbits;

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        beforePropagationStepsText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        afterPropagationStepsText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        propagationStepPeriodText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        recomputeFullDataIntervalText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            Integer.parseInt(beforePropagationStepsText.getText());
            Integer.parseInt(afterPropagationStepsText.getText());
            Integer.parseInt(propagationStepPeriodText.getText());
            Integer.parseInt(recomputeFullDataIntervalText.getText());

            if(performanceAssessmentPoor()) {
                setWarning(" Potential performance issues ");
            } else {
                setWarning(null);
            }

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void setWarning(String message) {
        warningLabel.setText(Objects.requireNonNullElse(message, ""));
        warningImage.setVisible(message != null);
        warningLabel.setVisible(message != null);
    }

    private boolean performanceAssessmentPoor() {
        // Compute total points per orbit
        long totalPoints = (Integer.parseInt(beforePropagationStepsText.getText()) + Integer.parseInt(afterPropagationStepsText.getText())) * nbOrbits;
        return totalPoints > 5000;
    }

    private void initialise(OrbitParameterConfiguration p, int numOrbits) {
        this.nbOrbits = numOrbits;
        beforePropagationStepsText.setText(String.valueOf(p.getBeforePropagationSteps()));
        afterPropagationStepsText.setText(String.valueOf(p.getAfterPropagationSteps()));
        propagationStepPeriodText.setText(String.valueOf(p.getStepInterval()));
        recomputeFullDataIntervalText.setText(String.valueOf(p.getRecomputeFullDataInterval()));
    }

    public OrbitParameterConfiguration getResult() {
        return new OrbitParameterConfiguration(Integer.parseInt(beforePropagationStepsText.getText()),
                Integer.parseInt(afterPropagationStepsText.getText()),
                Integer.parseInt(propagationStepPeriodText.getText()),
                Integer.parseInt(recomputeFullDataIntervalText.getText())
                );
    }

    public static OrbitParameterConfiguration openDialog(Window owner, OrbitParameterConfiguration p, int numOrbits) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Orbit Parameters");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OrbitConfigurationDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/OrbitConfigurationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            OrbitConfigurationDialog controller = loader.getController();
            if(p != null) {
                controller.initialise(p, numOrbits);
            }

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
}
