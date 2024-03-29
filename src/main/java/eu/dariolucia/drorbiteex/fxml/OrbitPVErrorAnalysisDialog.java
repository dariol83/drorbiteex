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

import eu.dariolucia.drorbiteex.model.collinearity.OrbitPVErrorAnalysisRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class OrbitPVErrorAnalysisDialog implements Initializable {

    private static Date lastStartDate = new Date(); // now
    private static Date lastEndDate = new Date(lastStartDate.getTime() + (24 * 3600 * 1000)); // 1 day
    private static final List<String> lastSelectedOrbitNames = new LinkedList<>();
    private static int lastPointInterval = 5; // in seconds

    public DatePicker startDatePicker;
    public TextField startTimeText;
    public DatePicker endDatePicker;
    public TextField endTimeText;

    public ListView<OrbitWrapper> orbitList;
    public TextField intervalPeriodText;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    private String error;

    private Orbit referenceOrbit;
    private Dialog<?> dialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        orbitList.setCellFactory(CheckBoxListCell.forListView(OrbitWrapper::selectedProperty));

        ChangeListener<Object> validationBroker =  (prop, oldVal, newVal) -> validate();

        startDatePicker.valueProperty().addListener(validationBroker);
        startTimeText.textProperty().addListener(validationBroker);
        endDatePicker.valueProperty().addListener(validationBroker);
        endTimeText.textProperty().addListener(validationBroker);
        intervalPeriodText.textProperty().addListener(validationBroker);

        validate();
    }

    private void validate() {
        try {
            if(startDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("Start date field is blank");
            }
            if(startTimeText.getText().isBlank()) {
                throw new IllegalStateException("Start time field is blank");
            }
            if(endDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("End date field is blank");
            }
            if(endTimeText.getText().isBlank()) {
                throw new IllegalStateException("End time field is blank");
            }

            Integer.parseInt(intervalPeriodText.getText());

            DialogUtils.getDate(startDatePicker, startTimeText);
            DialogUtils.getDate(endDatePicker, endTimeText);

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public OrbitPVErrorAnalysisRequest getResult() {
        try {
            Date start = DialogUtils.getDate(startDatePicker, startTimeText);
            Date end = DialogUtils.getDate(endDatePicker, endTimeText);
            lastStartDate = start;
            lastEndDate = end;
            List<Orbit> orbits = orbitList.getItems().stream().filter(OrbitWrapper::isSelected).map(OrbitWrapper::getOrbit).collect(Collectors.toList());
            lastSelectedOrbitNames.clear();
            lastSelectedOrbitNames.addAll(orbits.stream().map(Orbit::getName).collect(Collectors.toList()));

            int pointInterval = Integer.parseInt(intervalPeriodText.getText());
            lastPointInterval = pointInterval;
            return new OrbitPVErrorAnalysisRequest(start, end, referenceOrbit, orbits, pointInterval);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static OrbitPVErrorAnalysisRequest openDialog(Window owner, Orbit gs, List<Orbit> orbits) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Run orbit error analysis for " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OrbitPVErrorAnalysisDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/OrbitPVErrorAnalysisDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            OrbitPVErrorAnalysisDialog controller = loader.getController();
            controller.initialise(d, gs, orbits);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

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

    private void initialise(Dialog<?> d, Orbit gs, List<Orbit> orbits) {
        this.dialog = d;
        this.referenceOrbit = gs;
        this.startDatePicker.setValue(DialogUtils.toDateText(lastStartDate));
        this.startTimeText.setText(DialogUtils.toTimeText(lastStartDate));
        this.endDatePicker.setValue(DialogUtils.toDateText(lastEndDate));
        this.endTimeText.setText(DialogUtils.toTimeText(lastEndDate));
        this.intervalPeriodText.setText(String.valueOf(lastPointInterval));

        for(Orbit o : orbits) {
            OrbitWrapper ow = new OrbitWrapper(o);
            if(lastSelectedOrbitNames.contains(ow.getOrbit().getName())) {
                ow.selectedProperty().set(true);
            }
            this.orbitList.getItems().add(ow);
        }

        validate();
    }
}
