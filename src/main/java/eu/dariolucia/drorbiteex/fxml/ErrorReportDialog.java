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
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Pair;

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
            ChartManager cm = new ChartManager(chartName, chartParent);
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

    private class ChartManager {
        private final LineChart<Number, Number> errorChart;
        private final NumberAxis errorTimeAxis;
        private final NumberAxis errorValueAxis;
        private final Label mousePositionLabel;
        private boolean autorange = true;

        public ChartManager(String chartName, VBox parent) {
            this.mousePositionLabel = new Label("");
            this.mousePositionLabel.setTextAlignment(TextAlignment.LEFT);
            this.mousePositionLabel.setAlignment(Pos.CENTER_LEFT);
            this.mousePositionLabel.setMinSize(CHART_WIDTH, 24);
            this.mousePositionLabel.setMaxSize(CHART_WIDTH, 24);
            this.mousePositionLabel.setPrefSize(CHART_WIDTH, 24);

            this.errorTimeAxis = new NumberAxis();
            this.errorValueAxis = new NumberAxis();
            this.errorTimeAxis.setAutoRanging(false);
            this.errorTimeAxis.setTickLabelFormatter(longFormatter);
            this.errorTimeAxis.setMinorTickVisible(false);
            this.errorTimeAxis.setTickUnit(3600000);
            this.errorValueAxis.setAutoRanging(false);
            this.errorValueAxis.setMinorTickVisible(false);


            this.errorChart = new LineChart<>(this.errorTimeAxis, this.errorValueAxis);
            this.errorChart.setLegendVisible(true);
            this.errorChart.setLegendSide(Side.RIGHT);
            this.errorChart.setCreateSymbols(false);
            this.errorChart.setTitle(chartName);
            this.errorChart.setMinSize(CHART_WIDTH, CHART_HEIGHT);
            this.errorChart.setMaxSize(CHART_WIDTH, CHART_HEIGHT);
            this.errorChart.setPrefSize(CHART_WIDTH, CHART_HEIGHT);
            this.errorChart.setOnContextMenuRequested(e -> {
                ContextMenu m = new ContextMenu();
                for(XYChart.Series<Number, Number> s : this.errorChart.getData()) {
                    CheckMenuItem mItem = new CheckMenuItem(s.getName());
                    mItem.setSelected(s.getNode().isVisible());
                    mItem.setOnAction(o -> s.getNode().setVisible(mItem.isSelected()));
                    m.getItems().add(mItem);
                }
                m.getItems().add(new SeparatorMenuItem());
                final MenuItem copyItem = new MenuItem("Copy image to clipboard");
                copyItem.setOnAction(event -> {
                    WritableImage image = new WritableImage((int) this.errorChart.getWidth(), (int) this.errorChart.getHeight());
                    image = this.errorChart.snapshot(null, image);
                    ClipboardContent content = new ClipboardContent();
                    content.putImage(image);
                    Clipboard.getSystemClipboard().setContent(content);
                });
                m.getItems().add(copyItem);
                m.getItems().add(new SeparatorMenuItem());
                final MenuItem yRangeItem = new MenuItem("Set value range...");
                yRangeItem.setOnAction(this::openRangeSettingDialog);
                m.getItems().add(yRangeItem);

                m.show(this.errorChart.getScene().getWindow(), e.getScreenX(), e.getScreenY());
            });
            this.errorChart.setOnMouseMoved((MouseEvent event) -> {
                Point2D mouseSceneCoords = new Point2D(event.getSceneX(), event.getSceneY());
                double x = errorTimeAxis.sceneToLocal(mouseSceneCoords).getX();
                double y = errorValueAxis.sceneToLocal(mouseSceneCoords).getY();
                String time = longFormatter.toString(errorTimeAxis.getValueForDisplay(x));
                double value = errorValueAxis.getValueForDisplay(y).doubleValue();
                mousePositionLabel.setText(time + "\t\t" + value);
            });
            this.errorChart.setOnMouseExited((MouseEvent event) -> {
                mousePositionLabel.setText("");
            });

            parent.getChildren().add(this.errorChart);
            parent.getChildren().add(this.mousePositionLabel);
        }

        private void openRangeSettingDialog(ActionEvent event) {
            Bounds bounds = errorChart.getBoundsInLocal();
            Bounds screenBounds = errorChart.localToScreen(bounds);
            double x = screenBounds.getMinX();
            double y = screenBounds.getMinY();
            // double height = screenBounds.getHeight();
            RangePickerPanel.openDialog(new Pair<>(errorValueAxis.getLowerBound(), errorValueAxis.getUpperBound()),
                    this::setRange,
                    () -> this.autorange = true,
                    new Point2D(x, y));
        }

        private void setRange(Pair<Double, Double> doubleDoublePair) {
            this.autorange = false;
            this.errorValueAxis.setTickUnit((doubleDoublePair.getValue() - doubleDoublePair.getKey()) / 10.0);
            this.errorValueAxis.setLowerBound(doubleDoublePair.getKey());
            this.errorValueAxis.setUpperBound(doubleDoublePair.getValue());
        }

        public void updateChartRange(long min, long max) {
            errorTimeAxis.setAutoRanging(false);
            errorTimeAxis.setLowerBound(min);
            errorTimeAxis.setUpperBound(max);
            // tick unit is in fraction of ten
            errorTimeAxis.setTickUnit((max - min) / 10.0);
            if(autorange) {
                // compute yValue min max
                double yMin = Double.MAX_VALUE, yMax = Double.MIN_VALUE;
                for (XYChart.Series<Number, Number> s : errorChart.getData()) {
                    for (XYChart.Data<Number, Number> d : s.getData()) {
                        if (d.getXValue().longValue() < min) {
                            continue;
                        }
                        if (d.getXValue().longValue() > max) {
                            break;
                        }
                        if (yMin > d.getYValue().doubleValue()) {
                            yMin = d.getYValue().doubleValue();
                        }
                        if (yMax < d.getYValue().doubleValue()) {
                            yMax = d.getYValue().doubleValue();
                        }
                    }
                }
                errorValueAxis.setTickUnit((yMax - yMin) / 10.0);
                errorValueAxis.setLowerBound(yMin - (yMax - yMin) / 100.0);
                errorValueAxis.setUpperBound(yMax + (yMax - yMin) / 100.0);
            }
        }

        public void add(XYChart.Series<Number, Number> series) {
            errorChart.getData().add(series);
        }
    }
}
