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

import eu.dariolucia.drorbiteex.model.collinearity.ErrorPoint;
import eu.dariolucia.drorbiteex.model.determination.TleOrbitDeterminationRequest;
import eu.dariolucia.drorbiteex.model.determination.TleOrbitDeterminationResult;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.tle.TleGenerationRequest;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class TleOrbitDeterminationReportDialog implements Initializable {

    public Label textLabel;
    public TextArea tleTextArea;
    public VBox chartParent;
    private OrbitPane orbitPane;
    private Orbit orbit;
    private ChartManager residualChartManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing to do
    }

    public static void openDialog(Window owner, TleOrbitDeterminationResult result, TleOrbitDeterminationRequest request, OrbitPane orbitPane) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("TLE orbit determination result for orbit " + request.getOrbit().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TleOrbitDeterminationReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/TleOrbitDeterminationReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            TleOrbitDeterminationReportDialog controller = loader.getController();
            controller.configure(result, request, orbitPane);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configure(TleOrbitDeterminationResult result, TleOrbitDeterminationRequest request, OrbitPane orbitPane) {
        this.textLabel.setText("TLE orbit determination for Orbit " + request.getOrbit().getName());
        this.tleTextArea.setText(result.getEstimatedTle());
        this.orbit = request.getOrbit();
        this.orbitPane = orbitPane;

        // Create the chart managers based on the dataset labels
        this.residualChartManager = new ChartManager("Residual Error", chartParent, new NumberToDateAxisFormatter());

        // Get min-max time range
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        // You have one value for the error
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Residual Error");
        for (ErrorPoint tp : result.getResiduals()) {
            series.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getErrorAt(2)));
            if(tp.getTime().toEpochMilli() < minTime) {
                minTime = tp.getTime().toEpochMilli();
            }
            if(tp.getTime().toEpochMilli() > maxTime) {
                maxTime = tp.getTime().toEpochMilli();
            }
        }
        this.residualChartManager.add(series);
        this.residualChartManager.updateChartRange(minTime, maxTime);
        this.residualChartManager.setLegendVisible(false);
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
