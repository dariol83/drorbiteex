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

import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalysisRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CollinearityAnalysisDialog implements Initializable {

    private static Date lastStartDate = new Date(); // now
    private static Date lastEndDate = new Date(lastStartDate.getTime() + (7 * 24 * 3600 * 1000)); // 7 days

    private static String lastSelectedOrbitNames = null;

    private static double lastMinAngularSeparation = 5.0; // in degrees
    private static int lastPointInterval = 5; // in seconds

    private static int lastNbOfCores = 1;

    public TextField startDateText;
    public TextField startTimeText;
    public TextField endDateText;
    public TextField endTimeText;

    public ListView<Orbit> orbitList;
    public TextField minAngularSeparationText;
    public TextField intervalPeriodText;
    public Slider coreSlide;

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    private GroundStation groundStation;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        orbitList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        coreSlide.setMin(1);
        coreSlide.setMax(Runtime.getRuntime().availableProcessors());
        coreSlide.setMajorTickUnit(1);
        coreSlide.setMinorTickCount(0);
        coreSlide.setSnapToTicks(true);
        coreSlide.setBlockIncrement(1.0);

        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        startDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        coreSlide.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        minAngularSeparationText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        intervalPeriodText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            if(startDateText.getText().isBlank()) {
                throw new IllegalStateException("Start date field is blank");
            }
            if(startTimeText.getText().isBlank()) {
                throw new IllegalStateException("Start time field is blank");
            }
            if(endDateText.getText().isBlank()) {
                throw new IllegalStateException("End date field is blank");
            }
            if(endTimeText.getText().isBlank()) {
                throw new IllegalStateException("End time field is blank");
            }
            if(orbitList.getSelectionModel().getSelectedItem() == null) {
                throw new IllegalStateException("Orbit not selected");
            }
            Double.parseDouble(minAngularSeparationText.getText());
            Integer.parseInt(intervalPeriodText.getText());

            getDate(startDateText, startTimeText);
            getDate(endDateText, endTimeText);

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public CollinearityAnalysisRequest getResult() {
        try {
            Date start = getDate(startDateText, startTimeText);
            Date end = getDate(endDateText, endTimeText);
            lastStartDate = start;
            lastEndDate = end;
            Orbit orbit = this.orbitList.getSelectionModel().getSelectedItem();
            lastSelectedOrbitNames = orbit.getName();
            double minAngSep = Double.parseDouble(minAngularSeparationText.getText());
            lastMinAngularSeparation = minAngSep;
            int pointInterval = Integer.parseInt(intervalPeriodText.getText());
            lastPointInterval = pointInterval;
            int nbCores = (int) coreSlide.getValue();
            lastNbOfCores = nbCores;
            return new CollinearityAnalysisRequest(this.groundStation, orbit, start, end, minAngSep,
                    pointInterval, nbCores);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Date getDate(TextField dateText, TextField timeText) throws ParseException {
        return dateTimeFormatter.parse(dateText.getText() + " " + timeText.getText());
    }

    public static CollinearityAnalysisRequest openDialog(Window owner, GroundStation gs, List<Orbit> orbits) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Run collinearity analysis for " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = CollinearityAnalysisDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/CollinearityAnalysisDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CollinearityAnalysisDialog controller = loader.getController();
            controller.initialise(gs, orbits);

            d.getDialogPane().setContent(root);
            Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
            ok.disableProperty().bind(controller.validData.not());
            Optional<ButtonType> result = d.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                return controller.getResult();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initialise(GroundStation gs, List<Orbit> orbits) {
        this.groundStation = gs;
        this.startDateText.setText(toDateText(lastStartDate));
        this.startTimeText.setText(toTimeText(lastStartDate));
        this.endDateText.setText(toDateText(lastEndDate));
        this.endTimeText.setText(toTimeText(lastEndDate));
        this.intervalPeriodText.setText(String.valueOf(lastPointInterval));
        this.minAngularSeparationText.setText(String.valueOf(lastMinAngularSeparation));
        this.coreSlide.setValue(lastNbOfCores);
        Orbit selectedOrbit = null;
        for(Orbit o : orbits) {
            this.orbitList.getItems().add(o);
            if(lastSelectedOrbitNames != null && lastSelectedOrbitNames.equals(o.getName())) {
                selectedOrbit = o;
            }
        }
        if(selectedOrbit != null) {
            this.orbitList.getSelectionModel().select(selectedOrbit);
        }
        validate();
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

}
