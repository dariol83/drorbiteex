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

import javafx.event.ActionEvent;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

class ChartManager {
    private final LineChart<Number, Number> errorChart;
    private final NumberAxis errorTimeAxis;
    private final NumberAxis errorValueAxis;
    private final Label mousePositionLabel;
    private boolean autorange = true;

    private final NumberToDateAxisFormatter longFormatter;

    public ChartManager(String chartName, VBox parent, NumberToDateAxisFormatter formatter) {
        this.longFormatter = formatter;

        this.mousePositionLabel = new Label("");
        this.mousePositionLabel.setTextAlignment(TextAlignment.LEFT);
        this.mousePositionLabel.setAlignment(Pos.CENTER_LEFT);
        this.mousePositionLabel.setMinSize(ErrorReportDialog.CHART_WIDTH, 24);
        this.mousePositionLabel.setMaxSize(ErrorReportDialog.CHART_WIDTH, 24);
        this.mousePositionLabel.setPrefSize(ErrorReportDialog.CHART_WIDTH, 24);

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
        this.errorChart.setMinSize(ErrorReportDialog.CHART_WIDTH, ErrorReportDialog.CHART_HEIGHT);
        this.errorChart.setMaxSize(ErrorReportDialog.CHART_WIDTH, ErrorReportDialog.CHART_HEIGHT);
        this.errorChart.setPrefSize(ErrorReportDialog.CHART_WIDTH, ErrorReportDialog.CHART_HEIGHT);
        this.errorChart.setOnContextMenuRequested(e -> {
            ContextMenu m = new ContextMenu();
            for (XYChart.Series<Number, Number> s : this.errorChart.getData()) {
                CheckMenuItem mItem = new CheckMenuItem(s.getName());
                mItem.setSelected(s.getNode().isVisible());
                mItem.setOnAction(o -> {
                    s.getNode().setVisible(mItem.isSelected());
                    updateChartRange(this.errorTimeAxis.getLowerBound(), this.errorTimeAxis.getUpperBound());
                });
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

    public void updateChartRange(Number min, Number max) {
        if (min == null || max == null) {
            return;
        }
        errorTimeAxis.setAutoRanging(false);
        errorTimeAxis.setLowerBound(min.longValue());
        errorTimeAxis.setUpperBound(max.longValue());
        // tick unit is in fraction of ten
        errorTimeAxis.setTickUnit((max.longValue() - min.longValue()) / 10.0);
        if (autorange) {
            // compute yValue min max
            double yMin = Double.MAX_VALUE, yMax = Double.MIN_VALUE;
            for (XYChart.Series<Number, Number> s : errorChart.getData()) {
                if (s.getNode().isVisible()) {
                    for (XYChart.Data<Number, Number> d : s.getData()) {
                        if (d.getXValue().longValue() < min.longValue()) {
                            continue;
                        }
                        if (d.getXValue().longValue() > max.longValue()) {
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
            }
            if (yMax != Double.MIN_VALUE && yMin != Double.MAX_VALUE) {
                errorValueAxis.setTickUnit((yMax - yMin) / 10.0);
                errorValueAxis.setLowerBound(yMin - (yMax - yMin) / 100.0);
                errorValueAxis.setUpperBound(yMax + (yMax - yMin) / 100.0);
            }
        }
    }

    public void add(XYChart.Series<Number, Number> series) {
        errorChart.getData().add(series);
    }

    public void setLegendVisible(boolean b) {
        this.errorChart.setLegendVisible(b);
    }
}
