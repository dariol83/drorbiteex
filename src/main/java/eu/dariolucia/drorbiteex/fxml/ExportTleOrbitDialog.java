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

import eu.dariolucia.drorbiteex.model.orbit.OemOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.tle.TleGenerationRequest;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.TimeScalesFactory;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ExportTleOrbitDialog implements Initializable {

    public DatePicker startDatePicker;
    public TextField startTimeText;
    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextField satNumberText;
    public TextField classificationText;
    public TextField launchYearText;
    public TextField launchNumberText;
    public TextField launchPieceText;
    public TextField revolutionsText;
    public TextField elementNumberText;
    public DatePicker epochDatePicker;
    public TextField epochTimeText;
    public ComboBox<Orbit> tleOrbitLoadCombo;
    public Button tleOrbitLoadButton;
    private String error;
    private Orbit orbit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        satNumberText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        classificationText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        launchYearText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        launchNumberText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        launchPieceText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        revolutionsText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        elementNumberText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        epochDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        epochTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());

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
            if(epochDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("Start date field is blank");
            }
            if(epochTimeText.getText().isBlank()) {
                throw new IllegalStateException("Start time field is blank");
            }

            Integer.parseInt(satNumberText.getText());
            if(classificationText.getText().length() != 1) {
                throw new IllegalStateException("Classification not recognised: U or C or S");
            }
            if(classificationText.getText().charAt(0) != 'U' &&
                    classificationText.getText().charAt(0) != 'C' &&
                    classificationText.getText().charAt(0) != 'S') {
                throw new IllegalStateException("Classification not recognised: U or C or S");
            }
            Integer.parseInt(launchYearText.getText());
            Integer.parseInt(launchNumberText.getText());
            if(launchPieceText.getText().isEmpty()) {
                throw new IllegalStateException("Launch piece is blank");
            }
            Integer.parseInt(revolutionsText.getText());
            Integer.parseInt(elementNumberText.getText());

            DialogUtils.getDate(startDatePicker, startTimeText);
            DialogUtils.getDate(epochDatePicker, epochTimeText);
            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public TleGenerationRequest getResult() {
        try {
            Date start = DialogUtils.getDate(startDatePicker, startTimeText);
            Date epoch = DialogUtils.getDate(epochDatePicker, epochTimeText);
            return new TleGenerationRequest(orbit, start,
                    Integer.parseInt(satNumberText.getText()),
                    classificationText.getText().charAt(0),
                    Integer.parseInt(launchYearText.getText()),
                    Integer.parseInt(launchNumberText.getText()),
                    launchPieceText.getText(),
                    epoch,
                    Integer.parseInt(revolutionsText.getText()),
                    Integer.parseInt(elementNumberText.getText()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TleGenerationRequest openDialog(Window owner, Orbit gs, List<Orbit> orbitList) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Export TLE of " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = ExportTleOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/ExportTleOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            ExportTleOrbitDialog controller = loader.getController();
            controller.configure(gs, orbitList);

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

    private void configure(Orbit gs, List<Orbit> orbitList) {
        this.orbit = gs;
        Date refDate;
        if(gs.getModel() instanceof TleOrbitModel) {
            TLE originalTle = getTleFromOrbit(gs);
            refDate = originalTle.getDate().toDate(TimeScalesFactory.getUTC());
            // Set fields to read-only, update fields
            setTleFields(originalTle);

            satNumberText.setDisable(true);
            classificationText.setDisable(true);
            launchYearText.setDisable(true);
            launchNumberText.setDisable(true);
            launchPieceText.setDisable(true);
            revolutionsText.setDisable(true);
            elementNumberText.setDisable(true);
            epochTimeText.setDisable(true);
            epochDatePicker.setDisable(true);
            tleOrbitLoadCombo.setDisable(true);
            tleOrbitLoadButton.setDisable(true);
        } else {
            refDate = gs.getModel().getPropagator().getInitialState().getDate().toDate(TimeScalesFactory.getUTC());
            // Configure the TLE load button
            tleOrbitLoadCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(Orbit o) {
                    if(o == null) {
                        return "";
                    } else {
                        return o.getName();
                    }
                }

                @Override
                public Orbit fromString(String s) {
                    if(s.isBlank()) {
                        return null;
                    }
                    for(Orbit o : tleOrbitLoadCombo.getItems()) {
                        if(o.getName().equals(s)) {
                            return o;
                        }
                    }
                    return null;
                }
            });
            for(Orbit o : orbitList) {
                if(o.getModel() instanceof TleOrbitModel) {
                    this.tleOrbitLoadCombo.getItems().add(o);
                }
            }
        }
        this.startDatePicker.setValue(DialogUtils.toDateText(refDate));
        this.startTimeText.setText(DialogUtils.toTimeText(refDate));
    }

    private void setTleFields(TLE originalTle) {
        Date refDate = originalTle.getDate().toDate(TimeScalesFactory.getUTC());
        satNumberText.setText(String.valueOf(originalTle.getSatelliteNumber()));
        classificationText.setText(String.valueOf(originalTle.getClassification()));
        launchYearText.setText(String.valueOf(originalTle.getLaunchYear()));
        launchNumberText.setText(String.valueOf(originalTle.getLaunchNumber()));
        launchPieceText.setText(originalTle.getLaunchPiece());
        revolutionsText.setText(String.valueOf(originalTle.getRevolutionNumberAtEpoch()));
        elementNumberText.setText(String.valueOf(originalTle.getElementNumber()));
        epochDatePicker.setValue(DialogUtils.toDateText(refDate));
        epochTimeText.setText(DialogUtils.toTimeText(refDate));
    }

    public void onLoadDataFromTleOrbitAction(ActionEvent actionEvent) {
        Orbit selected = this.tleOrbitLoadCombo.getValue();
        if(selected != null) {
            TLE originalTle = getTleFromOrbit(selected);
            setTleFields(originalTle);
        }
    }

    private static TLE getTleFromOrbit(Orbit selected) {
        String tleFromModel = ((TleOrbitModel) selected.getModel()).getTle();
        String[] split = tleFromModel.split("\n", -1);
        return new TLE(split[0], split[1]);
    }
}
