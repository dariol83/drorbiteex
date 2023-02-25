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

import eu.dariolucia.drorbiteex.model.collinearity.TrackingErrorAnalysisRequest;
import eu.dariolucia.drorbiteex.model.collinearity.SkyCoverageAnalysisRequest;
import eu.dariolucia.drorbiteex.model.collinearity.TrackingErrorPoint;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TrackingErrorReportDialog implements Initializable {

    public Label periodLabel;
    public AreaChart<Long, Double> azimuthErrorChart;
    public NumberAxis azimuthErrorTimeAxis;
    public NumberAxis azimuthErrorValueAxis;
    public AreaChart<Long, Double> elevationErrorChart;
    public NumberAxis elevationErrorTimeAxis;
    public NumberAxis elevationErrorValueAxis;
    private TrackingErrorAnalysisRequest request;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //
        azimuthErrorTimeAxis.setTickLabelFormatter(new NumberToDateAxisFormatter());
        elevationErrorTimeAxis.setTickLabelFormatter(new NumberToDateAxisFormatter());
        azimuthErrorTimeAxis.setMinorTickVisible(false);
        elevationErrorTimeAxis.setMinorTickVisible(false);
        azimuthErrorTimeAxis.setTickUnit(3600000);
        elevationErrorTimeAxis.setTickUnit(3600000);
    }

    public static void openDialog(Window owner, TrackingErrorAnalysisRequest request, List<TrackingErrorPoint> points) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Tracking error result for " + request.getGroundStation().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TrackingErrorReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/TrackingErrorReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            TrackingErrorReportDialog controller = loader.getController();
            controller.initialise(request, points);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialise(TrackingErrorAnalysisRequest request, List<TrackingErrorPoint> points) {
        this.request = request;
        this.periodLabel.setText(TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()) + " - Reference orbit: " + request.getReferenceOrbit().getName() + " - "
        + "Target orbit: " + request.getTargetOrbit().getName());

        if(points.isEmpty()) {
            this.periodLabel.setText("No data collected");
            return;
        }

        // Get min-max time range
        long minTime = points.get(0).getTime().toEpochMilli();
        long maxTime = points.get(points.size() - 1).getTime().toEpochMilli();
        azimuthErrorTimeAxis.setAutoRanging(false);
        azimuthErrorTimeAxis.setLowerBound(minTime);
        azimuthErrorTimeAxis.setUpperBound(maxTime);
        elevationErrorTimeAxis.setAutoRanging(false);
        elevationErrorTimeAxis.setLowerBound(minTime);
        elevationErrorTimeAxis.setUpperBound(maxTime);

        // Create azimuth series
        XYChart.Series<Long, Double> azSeries = new XYChart.Series<>();
        azSeries.setName("Azimuth Error");
        for(TrackingErrorPoint tp : points) {
            azSeries.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getAzimuthError()));
        }
        azimuthErrorChart.getData().add(azSeries);

        // Create elevation series
        XYChart.Series<Long, Double> elSeries = new XYChart.Series<>();
        elSeries.setName("Elevation Error");
        for(TrackingErrorPoint tp : points) {
            elSeries.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getElevationError()));
        }
        elevationErrorChart.getData().add(elSeries);
    }
}
