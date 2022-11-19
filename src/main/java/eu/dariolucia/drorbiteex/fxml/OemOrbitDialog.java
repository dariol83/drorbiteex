/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.OemOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
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
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class OemOrbitDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextField filePathText;
    public TextArea oemTextArea;

    public ColorPicker colorPicker;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public ProgressIndicator oemProgress;

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        oemTextArea.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
            }
            if(oemTextArea.getText().isBlank()) {
                throw new IllegalStateException("OEM field is blank");
            }
            OemParser parser = new ParserBuilder().buildOemParser();
            parser.parse(new DataSource("oem", () -> new ByteArrayInputStream(oemTextArea.getText().getBytes(StandardCharsets.UTF_8))));
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
        colorPicker.setValue(Color.valueOf(gs.getColor()));
        if(gs.getModel() instanceof OemOrbitModel) {
            oemTextArea.setText(((OemOrbitModel) gs.getModel()).getOem());
        }
    }

    public Orbit getResult() {
        return new Orbit(UUID.randomUUID(), codeText.getText(), nameText.getText(), colorPicker.getValue().toString(), true, new OemOrbitModel(oemTextArea.getText()));
    }

    public static Orbit openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static Orbit openDialog(Window owner, Orbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("OEM Orbit");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OemOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/OemOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            OemOrbitDialog controller = loader.getController();
            if(gs != null) {
                controller.setOriginalOrbit(gs);
            }

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

    public void onSelectOemAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select OEM File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));

        File selected = fc.showOpenDialog(oemProgress.getScene().getWindow());
        if(selected != null) {
            filePathText.setText(selected.getAbsolutePath());
            // Read contents and copy in oemTextArea
            oemTextArea.setDisable(true);
            oemProgress.setVisible(true);
            ModelManager.runLater(() -> {
                try {
                    String newOem = readOem(selected);
                    Platform.runLater(() -> {
                        if (newOem != null) {
                            oemTextArea.setText(newOem);
                        }
                        oemTextArea.setDisable(false);
                        oemProgress.setVisible(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        oemTextArea.setText("--- Error reading file ---");
                        oemTextArea.setDisable(false);
                        oemProgress.setVisible(false);
                    });
                }
            });
        }
    }

    private String readOem(File selected) throws IOException {
        return Files.readString(selected.toPath(), StandardCharsets.UTF_8);
    }
}
