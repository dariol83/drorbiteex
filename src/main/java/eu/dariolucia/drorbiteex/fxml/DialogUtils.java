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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

public class DialogUtils {

    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

    public static void alert(String title, String headerText, String content) {
        dialog(Alert.AlertType.ERROR, title, headerText, content);
    }

    public static void info(String title, String headerText, String content) {
        dialog(Alert.AlertType.INFORMATION, title, headerText, content);
    }

    public static void dialog(Alert.AlertType type, String title, String headerText, String content) {
        Alert alert = new Alert(type);
        CssHolder.applyTo(alert.getDialogPane());
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String headerText, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        CssHolder.applyTo(alert.getDialogPane());
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Date getDate(DatePicker datePicker, TextField timeText) throws ParseException {
        return getDate(datePicker.getValue(), timeText.getText());
    }

    public static Date getDate(LocalDate date, String time) throws ParseException {
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = String.format("%02d/", date.get(ChronoField.DAY_OF_MONTH));
        dateStr += String.format("%02d/", date.get(ChronoField.MONTH_OF_YEAR));
        dateStr += date.getYear();
        return dateTimeFormatter.parse(dateStr + " " + time);
    }

    public static String toTimeText(Date date) {
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return timeFormatter.format(date);
    }

    public static LocalDate toDateText(Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }
}
