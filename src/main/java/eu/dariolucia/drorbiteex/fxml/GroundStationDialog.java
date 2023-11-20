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

import eu.dariolucia.drorbiteex.model.determination.Measurement;
import eu.dariolucia.drorbiteex.model.determination.TdmImporter;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.GroundStationMask;
import eu.dariolucia.drorbiteex.model.station.MaskEntry;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class GroundStationDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextField siteText;
    public TextArea descriptionTextArea;
    public TextField latitudeText;
    public TextField longitudeText;
    public TextField altitudeText;
    public ColorPicker colorPicker;

    public TextField maskAzimuthText;
    public TextField maskElevationText;
    public Button addMaskEntryButton;
    public Button removeMaskEntryButton;
    public Button importMaskButton;
    public ListView<MaskEntry> maskList;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        latitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        longitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        altitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        maskList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        maskList.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> updateMaskCells(b));
        validate();
    }

    private void updateMaskCells(MaskEntry entry) {
        maskAzimuthText.setText(String.valueOf(entry.getAzimuth()));
        maskElevationText.setText(String.valueOf(entry.getElevation()));
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
            }
            if(nameText.getText().isBlank()) {
                throw new IllegalStateException("Name field is blank");
            }
            Double.parseDouble(latitudeText.getText());
            Double.parseDouble(longitudeText.getText());
            Double.parseDouble(altitudeText.getText());
            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void setOriginalGroundStation(GroundStation gs) {
        codeText.setText(gs.getCode());
        nameText.setText(gs.getName());
        siteText.setText(gs.getSite());
        descriptionTextArea.setText(gs.getDescription());
        latitudeText.setText(String.valueOf(gs.getLatitude()));
        longitudeText.setText(String.valueOf(gs.getLongitude()));
        altitudeText.setText(String.valueOf(gs.getHeight()));
        colorPicker.setValue(Color.valueOf(gs.getColor()));
        setMask(gs);
    }

    private void setMask(GroundStation gs) {
        maskList.getItems().clear();
        if(gs.getMask() != null && !gs.getMask().getEntries().isEmpty()) {
            for (MaskEntry me : gs.getMask().getEntries()) {
                maskList.getItems().add(me);
            }
        }
    }

    public GroundStation getResult() {
        return new GroundStation(UUID.randomUUID(), codeText.getText(), nameText.getText(), siteText.getText(), descriptionTextArea.getText(),colorPicker.getValue().toString(), true,
                Double.parseDouble(latitudeText.getText()), Double.parseDouble(longitudeText.getText()), Double.parseDouble(altitudeText.getText()), toMask(maskList));
    }

    private GroundStationMask toMask(ListView<MaskEntry> maskList) {
        if(maskList.getItems().isEmpty()) {
            return null;
        } else {
            GroundStationMask gsm = new GroundStationMask();
            List<MaskEntry> entries = new LinkedList<>(maskList.getItems());
            Collections.sort(entries);
            gsm.setEntries(entries);
            return gsm;
        }
    }

    public static GroundStation openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static GroundStation openDialog(Window owner, GroundStation gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Ground Station");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = GroundStationDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/GroundStationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            GroundStationDialog controller = loader.getController();
            if(gs != null) {
                controller.setOriginalGroundStation(gs);
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

    public void onAddMaskEntryAction(ActionEvent actionEvent) {
        MaskEntry me = createMaskEntryFromValues();
        if(me != null) {
            // Add to list, replace azimuth value if one is identical
            MaskEntry toReplace = null;
            for(MaskEntry entry : maskList.getItems()) {
                if(entry.getAzimuth() == me.getAzimuth()) {
                    toReplace = entry;
                    break;
                }
            }
            if(toReplace != null) {
                maskList.getItems().remove(toReplace);
            }
            maskList.getItems().add(me);
            Collections.sort(maskList.getItems());
        }
    }

    private MaskEntry createMaskEntryFromValues() {
        try {
            double az = Double.parseDouble(maskAzimuthText.getText());
            double el = Double.parseDouble(maskElevationText.getText());
            // Clear text
            maskElevationText.setText("");
            maskAzimuthText.setText("");
            maskAzimuthText.requestFocus();
            return new MaskEntry(az, el);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void onRemoveMaskEntryAction(ActionEvent actionEvent) {
        if(maskList.getSelectionModel().getSelectedItem() != null) {
            maskList.getItems().remove(maskList.getSelectionModel().getSelectedItem());
        }
    }

    public void onImportMaskEntryAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select mask file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("All Files","*.*"));

        File selected = fc.showOpenDialog(maskList.getScene().getWindow());
        if(selected != null) {
            // Read contents
            try {
                List<String> lines = Files.readAllLines(selected.toPath());
                List<MaskEntry> entries = new LinkedList<>();
                for(String l : lines) {
                    l = l.trim();
                    if(l.isBlank() || l.startsWith(";") || l.startsWith("//") || l.startsWith("#")) {
                        continue;
                    }
                    l = l.replace(";", " ").replace(":", " ").replace("|", " ").replace(",", " ")
                            .replace("\t", " ");
                    String[] split = l.split(" ", -1);
                    if(split.length >= 2) {
                        double az = Double.parseDouble(split[0]);
                        double el = Double.parseDouble(split[1]);
                        entries.add(new MaskEntry(az, el));
                    }
                }
                Collections.sort(entries);
                if(!entries.isEmpty()) {
                    maskList.getItems().clear();
                    maskList.getItems().addAll(entries);
                }
            } catch (IOException e) {
                // No update
                e.printStackTrace();
            }
        }
    }
}
