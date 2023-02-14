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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class DateTimePickerPanel implements Initializable {

    public DatePicker datePicker;
    public Spinner<Integer> hours;
    public Spinner<Integer> minutes;
    public Spinner<Integer> seconds;
    private Consumer<Date> onApplyConsumer;
    private Stage stage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        hours.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23, 0));
        minutes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59, 0));
        seconds.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59, 0));
        datePicker.setValue(LocalDate.ofInstant(new Date().toInstant(), ZoneId.of("UTC")));
    }

    public void configure(Date dateTime, Consumer<Date> onApplyConsumer) {
        setDateTime(dateTime);
        this.onApplyConsumer = onApplyConsumer;
    }

    private void setDateTime(Date dateTime) {
        LocalDate ld = LocalDate.ofInstant(dateTime.toInstant(), ZoneId.of("UTC"));
        datePicker.setValue(ld);
        ZonedDateTime gc = ZonedDateTime.ofInstant(dateTime.toInstant(), ZoneId.of("UTC"));
        hours.getValueFactory().setValue(gc.get(ChronoField.HOUR_OF_DAY));
        minutes.getValueFactory().setValue(gc.get(ChronoField.MINUTE_OF_HOUR));
        seconds.getValueFactory().setValue(gc.get(ChronoField.SECOND_OF_MINUTE));
    }

    public void onApplyAction(ActionEvent actionEvent) {
        Date result = constructDateTime();
        if(onApplyConsumer != null) {
            onApplyConsumer.accept(result);
            onApplyConsumer = null;
        }
        // Close stage
        if(this.stage != null) {
            this.stage.close();
        }
    }

    private Date constructDateTime() {
        LocalDate ld = datePicker.getValue();
        String time = String.format("%02d", hours.getValue()) + ":" + String.format("%02d", minutes.getValue()) + ":" + String.format("%02d", seconds.getValue());
        try {
            return DialogUtils.getDate(ld, time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Stage openDialog(Window owner, Date toInitialise, Consumer<Date> resultConsumer, Point2D tlPosition) {
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
                    if(resultConsumer != null) {
                        resultConsumer.accept(null);
                    }
                    s.close();
                }
            });
            URL dataSelectionDialogFxmlUrl = DateTimePickerPanel.class.getResource("/eu/dariolucia/drorbiteex/fxml/DateTimePickerPanel.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            DateTimePickerPanel controller = loader.getController();
            if(toInitialise != null) {
                controller.configure(toInitialise, resultConsumer);
            }
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
