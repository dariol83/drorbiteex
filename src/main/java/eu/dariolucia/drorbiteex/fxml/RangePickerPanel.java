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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Pair;

import java.net.URL;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class RangePickerPanel implements Initializable {

    public TextField minText;
    public TextField maxText;
    public Button applyRangeButton;
    private Consumer<Pair<Double, Double>> onApplyRangeConsumer;
    private Runnable onApplyAutorangeConsumer;
    private Stage stage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.minText.textProperty().addListener((t,o,n) -> validate());
        this.maxText.textProperty().addListener((t,o,n) -> validate());
    }

    private void validate() {
        try {
            Double.parseDouble(this.minText.getText());
            Double.parseDouble(this.maxText.getText());

            applyRangeButton.setDisable(false);
        } catch (Exception e) {
            applyRangeButton.setDisable(true);
        }
    }

    public void configure(Pair<Double, Double> currentRange, Consumer<Pair<Double, Double>> onApplyRangeConsumer, Runnable onApplyAutorangeConsumer) {
        this.minText.setText(String.valueOf(currentRange.getKey()));
        this.maxText.setText(String.valueOf(currentRange.getValue()));
        this.onApplyAutorangeConsumer = onApplyAutorangeConsumer;
        this.onApplyRangeConsumer = onApplyRangeConsumer;
    }

    public void onApplyRangeAction(ActionEvent actionEvent) {
        if(onApplyRangeConsumer != null) {
            onApplyRangeConsumer.accept(new Pair<>(
                    Double.parseDouble(this.minText.getText()),
                    Double.parseDouble(this.maxText.getText()))
            );
            onApplyRangeConsumer = null;
        }
        // Close stage
        if(this.stage != null && this.stage.isShowing()) {
            this.stage.close();
            this.stage = null;
        }
    }

    public void onApplyAutorangeAction(ActionEvent actionEvent) {
        this.onApplyAutorangeConsumer.run();
        if(this.stage != null && this.stage.isShowing()) {
            this.stage.close();
            this.stage = null;
        }
    }

    public static Stage openDialog(Pair<Double, Double> currentRange, Consumer<Pair<Double, Double>> rangeConsumer, Runnable autorangeConsumer, Point2D tlPosition) {
        try {
            // Create the stage
            Stage s = new Stage();
            s.initStyle(StageStyle.UNDECORATED);
            s.setResizable(false);
            s.setAlwaysOnTop(false);
            s.setX(tlPosition.getX());
            s.setY(tlPosition.getY());
            s.focusedProperty().addListener((a,b,c) -> {
                if(!c) {
                    // Focus lost
                    s.close();
                }
            });
            URL dataSelectionDialogFxmlUrl = RangePickerPanel.class.getResource("/eu/dariolucia/drorbiteex/fxml/RangePickerPanel.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            root.getStyleClass().add("floating-panel");
            CssHolder.applyTo(root);
            RangePickerPanel controller = loader.getController();
            controller.configure(currentRange, rangeConsumer, autorangeConsumer);
            controller.setStage(s);
            s.setScene(new Scene(root));
            s.show();

            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setStage(Stage s) {
        if(this.stage != null) {
            throw new IllegalStateException("Stage already set");
        }
        this.stage = s;
    }

}
