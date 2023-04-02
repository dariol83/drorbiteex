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

import eu.dariolucia.drorbiteex.fxml.range.RangeSelector;
import eu.dariolucia.drorbiteex.model.collinearity.ErrorPoint;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ErrorReportDialog implements Initializable {

    public static final int CHART_WIDTH = 800;
    public static final int CHART_HEIGHT = 200;
    public Label periodLabel;
    public RangeSelector rangeSelectorController;
    public VBox chartParent;
    private NumberToDateAxisFormatter longFormatter;
    private final List<ChartManager> charts = new LinkedList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //
        longFormatter = new NumberToDateAxisFormatter();
        rangeSelectorController.rangeProperty().addListener((w,o,n) -> updateChartRange(n.getKey(), n.getValue()));
        rangeSelectorController.setLabelFormatter(longFormatter);
    }

    private void updateChartRange(long min, long max) {
        charts.forEach(cm -> cm.updateChartRange(min, max));
    }

    public static void openDialog(Window owner, String title, String description, String[] datasetLabels, Map<String, List<ErrorPoint>> points) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle(title);
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = ErrorReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/ErrorReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            ErrorReportDialog controller = loader.getController();
            controller.configure(description, datasetLabels, points);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configure(String description, String[] datasetLabels, Map<String, List<ErrorPoint>> points) {
        this.periodLabel.setText(description);

        if(points.isEmpty()) {
            this.periodLabel.setText("No data collected");
            return;
        }

        // Create the chart managers based on the dataset labels
        for(String chartName : datasetLabels) {
            ChartManager cm = new ChartManager(chartName, chartParent, longFormatter);
            this.charts.add(cm);
        }

        // Get min-max time range
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for(Map.Entry<String, List<ErrorPoint>> e : points.entrySet()) {
            if(!e.getValue().isEmpty()) {
                if(minTime > e.getValue().get(0).getTime().toEpochMilli()) {
                    minTime = e.getValue().get(0).getTime().toEpochMilli();
                }
                if(maxTime < e.getValue().get(e.getValue().size() - 1).getTime().toEpochMilli()) {
                    maxTime = e.getValue().get(e.getValue().size() - 1).getTime().toEpochMilli();
                }
            }

            // Create error series
            String itemName = e.getKey();
            List<ErrorPoint> errorPoints = e.getValue();
            // You have one value for each dataset
            // Inefficient
            for(int i = 0; i < datasetLabels.length; ++i) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(itemName);
                for (ErrorPoint tp : errorPoints) {
                    series.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getErrorAt(i)));
                }
                charts.get(i).add(series);
            }
        }
        rangeSelectorController.setBounds(minTime, maxTime);
        updateChartRange(minTime, maxTime);
    }
}
