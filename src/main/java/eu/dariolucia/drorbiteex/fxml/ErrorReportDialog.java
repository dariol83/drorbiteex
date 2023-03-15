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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ErrorReportDialog implements Initializable {

    public Label periodLabel;
    public LineChart<Long, Double> error1Chart;
    public NumberAxis error1TimeAxis;
    public NumberAxis error1ValueAxis;
    public LineChart<Long, Double> error2Chart;
    public NumberAxis error2TimeAxis;
    public NumberAxis error2ValueAxis;
    public RangeSelector rangeSelectorController;
    private NumberToDateAxisFormatter longFormatter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //
        longFormatter = new NumberToDateAxisFormatter();
        error1Chart.setCreateSymbols(false);
        error2Chart.setCreateSymbols(false);
        error1TimeAxis.setTickLabelFormatter(longFormatter);
        error2TimeAxis.setTickLabelFormatter(longFormatter);
        error1TimeAxis.setMinorTickVisible(false);
        error2TimeAxis.setMinorTickVisible(false);
        error1TimeAxis.setTickUnit(3600000);
        error2TimeAxis.setTickUnit(3600000);

        error1Chart.setLegendVisible(true);
        error2Chart.setLegendVisible(true);

        rangeSelectorController.rangeProperty().addListener((w,o,n) -> updateChartRange(n.getKey(), n.getValue()));
        rangeSelectorController.setLabelFormatter(longFormatter);

        attachMenu(error1Chart);
        attachMenu(error2Chart);
    }

    private void attachMenu(LineChart<Long, Double> chart) {
        chart.setOnContextMenuRequested(e -> {
            ContextMenu m = new ContextMenu();
            for(XYChart.Series<Long, Double> s : chart.getData()) {
                CheckMenuItem mItem = new CheckMenuItem(s.getName());
                mItem.setSelected(s.getNode().isVisible());
                mItem.setOnAction(o -> s.getNode().setVisible(mItem.isSelected()));
                m.getItems().add(mItem);
            }
            m.getItems().add(new SeparatorMenuItem());
            final MenuItem copyItem = new MenuItem("Copy image to clipboard");
            copyItem.setOnAction(event -> {
                WritableImage image = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
                image = chart.snapshot(null, image);
                ClipboardContent content = new ClipboardContent();
                content.putImage(image);
                Clipboard.getSystemClipboard().setContent(content);
            });
            m.getItems().add(copyItem);
            m.show(chart.getScene().getWindow(), e.getScreenX(), e.getScreenY());
        });
    }

    private void updateChartRange(long min, long max) {
        error1TimeAxis.setAutoRanging(false);
        error1TimeAxis.setLowerBound(min);
        error1TimeAxis.setUpperBound(max);
        error2TimeAxis.setAutoRanging(false);
        error2TimeAxis.setLowerBound(min);
        error2TimeAxis.setUpperBound(max);
        // tick unit is in fraction of ten
        error1TimeAxis.setTickUnit((max - min) / 10.0);
        error2TimeAxis.setTickUnit((max - min) / 10.0);
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

            // Create error1 series
            XYChart.Series<Long, Double> azSeries = new XYChart.Series<>();
            azSeries.setName(e.getKey());
            for(ErrorPoint tp : e.getValue()) {
                azSeries.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getError1()));
            }
            error1Chart.getData().add(azSeries);

            // Create error2 series
            XYChart.Series<Long, Double> elSeries = new XYChart.Series<>();
            elSeries.setName(e.getKey());
            for(ErrorPoint tp : e.getValue()) {
                elSeries.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getError2()));
            }
            error2Chart.getData().add(elSeries);
        }
        error1Chart.setTitle(datasetLabels[0]);
        error2Chart.setTitle(datasetLabels[1]);

        rangeSelectorController.setBounds(minTime, maxTime);
        updateChartRange(minTime, maxTime);
    }
}
