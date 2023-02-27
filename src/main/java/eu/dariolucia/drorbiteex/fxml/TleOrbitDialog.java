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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.orekit.propagation.analytical.tle.TLE;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.UUID;

public class TleOrbitDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextArea tleTextArea;
    public ColorPicker colorPicker;
    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public TextArea tleDetailsTextArea;

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        tleTextArea.textProperty().addListener((prop, oldVal, newVal) -> validate());
        tleTextArea.textProperty().addListener((prop, oldVal, newVal) -> showTleDetails());
        tleDetailsTextArea.setText("Invalid TLE data");

        validate();
    }

    private void showTleDetails() {
        try {
            String tle = tleTextArea.getText();
            String toShow = showTleDetails(tle);
            //
            tleDetailsTextArea.setText(toShow);
        } catch (Exception e) {
            tleDetailsTextArea.setText("Invalid TLE data");
        }
    }

    private static String mapEphemerisType(int type) {
        switch (type) {
            case TLE.DEFAULT: return "Default";
            case TLE.SGP: return "SGP";
            case TLE.SGP4: return "SGP4";
            case TLE.SGP8: return "SGP8";
            case TLE.SDP4: return "SDP4";
            case TLE.SDP8: return "SDP8";
            default: return "Unknown";
        }
    }

    public static String showTleDetails(String tle) {
        try {
            TLE tleObject = new TLE(tle.substring(0, tle.indexOf("\n")).trim(), tle.substring(tle.indexOf("\n")).trim());
            //
            StringBuilder sb = new StringBuilder();
            addLine(sb, tleObject.getSatelliteNumber(), "Satellite Number");
            addLine(sb, tleObject.getDate().toString(TimeZone.getTimeZone("UTC")), "TLE Date");
            addLine(sb, tleObject.getRevolutionNumberAtEpoch(), "Orbit Number");
            addLine(sb, tleObject.getE(), "Eccentricity");
            addLine(sb, tleObject.getBStar(), "Ballistic Coeff.");
            addLine(sb, tleObject.getClassification(), "Classification");
            addLine(sb, tleObject.getElementNumber(), "Element Number");
            addLine(sb, mapEphemerisType(tleObject.getEphemerisType()), "Ephemeris Type");
            addLine(sb, tleObject.getLaunchNumber(), "Launch Number");
            addLine(sb, tleObject.getLaunchPiece(), "Launch Piece");
            addLine(sb, tleObject.getLaunchYear(), "Launch Year");
            addLine(sb, tleObject.getMeanAnomaly(), "Mean Anomaly");
            addLine(sb, tleObject.getMeanMotion(), "Mean Motion");
            addLine(sb, tleObject.getMeanMotionFirstDerivative(), "Mean Motion'");
            addLine(sb, tleObject.getMeanMotionSecondDerivative(), "Mean Motion''");
            addLine(sb, tleObject.getPerigeeArgument(), "Perigee");
            addLine(sb, tleObject.getRaan(), "RAAN");
            //
            return sb.toString();
        } catch (Exception e) {
            return "Invalid TLE data";
        }
    }

    private static void addLine(StringBuilder sb, String argument, String name) {
        sb.append(String.format("%-20s:\t%s%n", name, argument));
    }

    private static void addLine(StringBuilder sb, double argument, String name) {
        sb.append(String.format("%-20s:\t%f%n", name, argument));
    }

    private static void addLine(StringBuilder sb, int argument, String name) {
        sb.append(String.format("%-20s:\t%d%n", name, argument));
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
            }
            if(nameText.getText().isBlank()) {
                throw new IllegalStateException("Name field is blank");
            }
            if(tleTextArea.getText().isBlank()) {
                throw new IllegalStateException("TLE field is blank");
            }
            String tle = tleTextArea.getText();
            new TLE(tle.substring(0, tle.indexOf("\n")).trim(), tle.substring(tle.indexOf("\n")).trim());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void setOriginalOrbit(Orbit gs) {
        codeText.setText(gs.getCode());
        nameText.setText(gs.getName());
        nameText.setDisable(true); // Let's avoid mess with change of names... only code is displayed anyway
        if(gs.getModel() instanceof TleOrbitModel) {
            tleTextArea.setText(((TleOrbitModel) gs.getModel()).getTle());
        }
        colorPicker.setValue(Color.valueOf(gs.getColor()));
    }

    public Orbit getResult() {
        return new Orbit(UUID.randomUUID(), codeText.getText(), nameText.getText(), colorPicker.getValue().toString(), true, new TleOrbitModel(tleTextArea.getText()));
    }

    public static Orbit openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static Orbit openDialog(Window owner, Orbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("TLE Orbit");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TleOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/TleOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            TleOrbitDialog controller = loader.getController();
            if(gs != null) {
                controller.setOriginalOrbit(gs);
            }

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
}
