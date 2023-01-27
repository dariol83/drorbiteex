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
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import java.util.stream.Collectors;

public class CollinearityAnalysisDialog implements Initializable {

    private static Date lastStartDate = new Date(); // now
    private static Date lastEndDate = new Date(lastStartDate.getTime() + (24 * 3600 * 1000)); // 1 day
    private static String lastSelectedOrbitNames = null;
    private static double lastMinAngularSeparation = 5.0; // in degrees
    private static int lastPointInterval = 5; // in seconds
    private static int lastNbOfCores = 1;
    private static final List<String> lastExclusions = new LinkedList<>();
    private static Integer lastMaxHeight = null;
    private static Integer lastMinHeight = null;
    private static boolean lastCelestrakOrbits = true;
    private static String lastCelestrakGroup = "active";

    public TextField startDateText;
    public TextField startTimeText;
    public TextField endDateText;
    public TextField endTimeText;

    public ComboBox<Orbit> orbitList;
    public TextField minAngularSeparationText;
    public TextField intervalPeriodText;
    public Slider coreSlide;

    // Exclusion part
    public TextField exclusionText;
    public Button addExclusionButton;
    public Button removeExclusionButton;
    public ListView<String> exclusionList;

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField maxHeightText;
    public TextField minHeightText;
    public RadioButton celestrakGroupRadio;
    public ComboBox<String> celestrakGroupCombo;
    public RadioButton applicationGroupRadio;
    private String error;

    private GroundStation groundStation;
    private Dialog<?> dialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        celestrakGroupCombo.getItems().addAll(CelestrakTleData.CELESTRAK_GROUPS);
        ToggleGroup radioGroup = new ToggleGroup();
        celestrakGroupRadio.setToggleGroup(radioGroup);
        applicationGroupRadio.setToggleGroup(radioGroup);
        celestrakGroupRadio.setSelected(true);
        celestrakGroupCombo.getSelectionModel().select("active");
        celestrakGroupCombo.disableProperty().bind(celestrakGroupRadio.selectedProperty().not());

        coreSlide.setMin(1);
        coreSlide.setMax(Runtime.getRuntime().availableProcessors());
        coreSlide.setMajorTickUnit(1);
        coreSlide.setMinorTickCount(0);
        coreSlide.setSnapToTicks(true);
        coreSlide.setBlockIncrement(1.0);

        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        ChangeListener<Object> validationBroker =  (prop, oldVal, newVal) -> validate();

        startDateText.textProperty().addListener(validationBroker);
        startTimeText.textProperty().addListener(validationBroker);
        endDateText.textProperty().addListener(validationBroker);
        endTimeText.textProperty().addListener(validationBroker);
        maxHeightText.textProperty().addListener(validationBroker);
        minHeightText.textProperty().addListener(validationBroker);
        coreSlide.valueProperty().addListener(validationBroker);

        minAngularSeparationText.textProperty().addListener(validationBroker);
        intervalPeriodText.textProperty().addListener(validationBroker);
        orbitList.getSelectionModel().selectedItemProperty().addListener(validationBroker);

        exclusionText.textProperty().addListener((w, o, n) -> changeFocus());
        addExclusionButton.disableProperty().bind(exclusionText.textProperty().isEmpty());
        removeExclusionButton.disableProperty().bind(exclusionList.getSelectionModel().selectedItemProperty().isNull());

        validate();
    }

    private void changeFocus() {
        if(exclusionText.isFocused() && !exclusionText.getText().isBlank()) {
            addExclusionButton.setDefaultButton(true);
        } else {
            addExclusionButton.setDefaultButton(false);
            Button ok = (Button) this.dialog.getDialogPane().lookupButton(ButtonType.OK);
            ok.setDefaultButton(true);
        }
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

            if(!maxHeightText.getText().isBlank()) {
                Integer.parseInt(maxHeightText.getText());
            }
            if(!minHeightText.getText().isBlank()) {
                Integer.parseInt(minHeightText.getText());
            }

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
            lastExclusions.clear();
            lastExclusions.addAll(exclusionList.getItems());
            Integer minHeight = null;
            Integer maxHeight = null;
            if(!minHeightText.getText().isBlank()) {
                minHeight = Integer.parseInt(minHeightText.getText());
            }
            if(!maxHeightText.getText().isBlank()) {
                maxHeight = Integer.parseInt(maxHeightText.getText());
            }
            lastMinHeight = minHeight;
            lastMaxHeight = maxHeight;
            lastCelestrakOrbits = celestrakGroupRadio.isSelected();
            lastCelestrakGroup = celestrakGroupCombo.getSelectionModel().getSelectedItem();
            return new CollinearityAnalysisRequest(this.groundStation, orbit, start, end, minAngSep,
                    pointInterval, nbCores, List.copyOf(exclusionList.getItems()), minHeight, maxHeight,
                    celestrakGroupRadio.isSelected() ? celestrakGroupCombo.getSelectionModel().getSelectedItem() : null,
                    applicationGroupRadio.isSelected() ? getTargetOrbitList(this.orbitList.getItems(), orbit) : null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Orbit> getTargetOrbitList(ObservableList<Orbit> items, Orbit toRemove) {
        return items.stream().filter(o -> !o.getId().equals(toRemove.getId())).collect(Collectors.toCollection(LinkedList::new));
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
            controller.initialise(d, gs, orbits);

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

    private void initialise(Dialog<?> d, GroundStation gs, List<Orbit> orbits) {
        this.dialog = d;
        this.groundStation = gs;
        this.startDateText.setText(toDateText(lastStartDate));
        this.startTimeText.setText(toTimeText(lastStartDate));
        this.endDateText.setText(toDateText(lastEndDate));
        this.endTimeText.setText(toTimeText(lastEndDate));
        this.intervalPeriodText.setText(String.valueOf(lastPointInterval));
        this.minAngularSeparationText.setText(String.valueOf(lastMinAngularSeparation));
        this.coreSlide.setValue(lastNbOfCores);
        this.exclusionList.getItems().addAll(lastExclusions);
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
        if(lastMinHeight != null) {
            this.minHeightText.setText(String.valueOf(lastMinHeight.intValue()));
        }
        if(lastMaxHeight != null) {
            this.maxHeightText.setText(String.valueOf(lastMaxHeight.intValue()));
        }
        if(lastCelestrakOrbits) {
            this.celestrakGroupRadio.setSelected(true);
            this.celestrakGroupCombo.getSelectionModel().select(lastCelestrakGroup);
        } else {
            this.applicationGroupRadio.setSelected(true);
        }
        validate();
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

    public void onAddExclusionAction(ActionEvent actionEvent) {
        if(!exclusionText.getText().isBlank()) {
            exclusionList.getItems().add(exclusionText.getText());
            exclusionText.setText("");
            exclusionText.requestFocus();
        }
    }

    public void onRemoveExclusionAction(ActionEvent actionEvent) {
        if(exclusionList.getSelectionModel().getSelectedItem() != null) {
            exclusionList.getItems().remove(exclusionList.getSelectionModel().getSelectedItem());
        }
    }
}
