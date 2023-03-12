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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.tle.TleGenerationRequest;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.ResourceBundle;

public class TleReportDialog implements Initializable {

    public Label textLabel;
    public TextArea tleTextArea;
    private OrbitPane orbitPane;
    private Orbit orbit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing to do
    }

    public static void openDialog(Window owner, String tle, TleGenerationRequest request, OrbitPane orbitPane) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("TLE result for orbit " + request.getOrbit().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TleReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/TleReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            TleReportDialog controller = loader.getController();
            controller.configure(tle, request, orbitPane);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configure(String tle, TleGenerationRequest request, OrbitPane orbitPane) {
        this.textLabel.setText("TLE for Orbit " + request.getOrbit().getName() + " at epoch time " + TimeUtils.formatDate(request.getStartTime()));
        this.tleTextArea.setText(tle);
        this.orbit = request.getOrbit();
        this.orbitPane = orbitPane;
    }

    public void onCopyTleButtonAction(ActionEvent actionEvent) {
        ClipboardContent content = new ClipboardContent();
        content.putString(this.tleTextArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void onAddTleButtonAction(ActionEvent actionEvent) {
        orbitPane.onNewOrbitAction(this.orbit, this.tleTextArea.getText());
    }
}
