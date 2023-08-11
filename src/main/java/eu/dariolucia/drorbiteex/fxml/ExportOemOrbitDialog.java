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

import eu.dariolucia.drorbiteex.model.oem.OemExporterRegistry;
import eu.dariolucia.drorbiteex.model.oem.OemGenerationRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class ExportOemOrbitDialog implements Initializable {

    private static int LAST_PERIOD = 30;
    private static long LAST_TIME_DIFFERENCE = 7 * 24 * 3600 * 1000L;
    private static String LAST_FRAME;
    private static String LAST_FORMAT;
    private static String LAST_POSTPROCESSOR;
    private static String LAST_GENERATOR;
    private static String LAST_FILE;
    private static String LAST_FOLDER;
    private static boolean LAST_FILE_SELECTED = true;

    public TextField codeText;
    public TextField nameText;
    public DatePicker startDatePicker;
    public TextField startTimeText;
    public DatePicker endDatePicker;
    public TextField endTimeText;
    public TextField periodText;
    public TextField filePathText;
    public ComboBox<String> frameCombo;
    public ComboBox<String> formatCombo;
    public ComboBox<String> postProcessorCombo;
    public RadioButton filePathRadio;
    public Button filePathButton;
    public RadioButton folderPathRadio;
    public TextField folderPathText;
    public ComboBox<String> fileGeneratorCombo;
    public Button folderPathButton;

    private boolean isTle = false;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;
    private Orbit orbit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        folderPathText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathRadio.selectedProperty().addListener((prop, oldVal, newVal) -> validate());
        folderPathRadio.selectedProperty().addListener((prop, oldVal, newVal) -> validate());
        formatCombo.getSelectionModel().select(0);
        frameCombo.getSelectionModel().select(0);

        periodText.setText(String.valueOf(LAST_PERIOD));

        for(String exporter : OemExporterRegistry.instance().getPostProcessors()) {
            postProcessorCombo.getItems().add(exporter);
        }
        postProcessorCombo.getSelectionModel().select(0);

        ToggleGroup fileTg = new ToggleGroup();
        filePathRadio.setToggleGroup(fileTg);
        folderPathRadio.setToggleGroup(fileTg);

        folderPathText.disableProperty().bind(folderPathRadio.selectedProperty().not());
        folderPathButton.disableProperty().bind(folderPathRadio.selectedProperty().not());
        fileGeneratorCombo.disableProperty().bind(folderPathRadio.selectedProperty().not());
        filePathText.disableProperty().bind(filePathRadio.selectedProperty().not());
        filePathButton.disableProperty().bind(filePathRadio.selectedProperty().not());

        for(String generator : OemExporterRegistry.instance().getNameGenerators()) {
            fileGeneratorCombo.getItems().add(generator);
        }
        fileGeneratorCombo.getSelectionModel().select(0);

        validate();
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
            }
            if(nameText.getText().isBlank()) {
                throw new IllegalStateException("Name field is blank");
            }
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
            if(filePathRadio.isSelected() && filePathText.getText().isBlank()) {
                throw new IllegalStateException("File is not selected");
            }
            if(folderPathRadio.isSelected() && folderPathText.getText().isBlank()) {
                throw new IllegalStateException("Folder is not selected");
            }
            if(!isTle && frameCombo.getSelectionModel().getSelectedItem().equals("TEME")) {
                throw new IllegalStateException("TEME reference frame can only be used with TLE orbits");
            }

            DialogUtils.getDate(startDatePicker, startTimeText);
            DialogUtils.getDate(endDatePicker, endTimeText);

            Integer.parseInt(periodText.getText());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public OemGenerationRequest getResult() {
        try {
            LAST_PERIOD = Integer.parseInt(periodText.getText());
            Date start = DialogUtils.getDate(startDatePicker, startTimeText);
            Date end = DialogUtils.getDate(endDatePicker, endTimeText);
            LAST_TIME_DIFFERENCE = end.getTime() - start.getTime();
            Frame frame = getFrame();
            FileFormat format = getFormat();
            LAST_FRAME = frameCombo.getValue();
            LAST_FORMAT = formatCombo.getValue();
            LAST_GENERATOR = fileGeneratorCombo.getValue();
            LAST_POSTPROCESSOR = postProcessorCombo.getValue();
            LAST_FILE = filePathText.getText();
            LAST_FOLDER = folderPathText.getText();
            LAST_FILE_SELECTED = filePathRadio.isSelected();

            return new OemGenerationRequest(orbit.copy().getModel().getPropagator(), codeText.getText(), nameText.getText(), start, end, Integer.parseInt(periodText.getText()),
                    filePathRadio.isSelected() ? filePathText.getText() : null,
                    frame,
                    format,
                    folderPathRadio.isSelected() ? folderPathText.getText() : null,
                    fileGeneratorCombo.getValue(),
                    postProcessorCombo.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private FileFormat getFormat() {
        switch (formatCombo.getSelectionModel().getSelectedItem()) {
            case "XML": return FileFormat.XML;
            case "KVN": return FileFormat.KVN;
            default: throw new IllegalStateException("Format not recognised: " + formatCombo.getSelectionModel().getSelectedItem());
        }
    }

    private Frame getFrame() {
        switch (frameCombo.getSelectionModel().getSelectedItem()) {
            case "ITRF": return FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            case "EME2000": return FramesFactory.getEME2000();
            case "GCRF": return FramesFactory.getGCRF();
            case "ICRF": return FramesFactory.getICRF();
            case "TOD": return FramesFactory.getTOD(true);
            case "TEME": return FramesFactory.getTEME();
            default: throw new IllegalStateException("Frame not recognised: " + frameCombo.getSelectionModel().getSelectedItem());
        }
    }

    public static OemGenerationRequest openDialog(Window owner, Orbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Export OEM of " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = ExportOemOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/ExportOemOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            ExportOemOrbitDialog controller = loader.getController();
            controller.initialise(gs);

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

    private void initialise(Orbit gs) {
        this.orbit = gs;
        this.nameText.setText(gs.getName());
        this.codeText.setText(gs.getCode());
        Date refDate = new Date();
        this.startDatePicker.setValue(DialogUtils.toDateText(refDate));
        this.startTimeText.setText(DialogUtils.toTimeText(refDate));
        this.endDatePicker.setValue(DialogUtils.toDateText(new Date(refDate.getTime() + LAST_TIME_DIFFERENCE)));
        this.endTimeText.setText(DialogUtils.toTimeText(new Date()));
        this.periodText.setText(String.valueOf(LAST_PERIOD));
        this.isTle = gs.getModel() instanceof TleOrbitModel;
        selectCombo(formatCombo, LAST_FORMAT);
        selectCombo(frameCombo, LAST_FRAME);
        selectCombo(postProcessorCombo, LAST_POSTPROCESSOR);
        selectCombo(fileGeneratorCombo, LAST_GENERATOR);
        if(LAST_FILE != null) {
            filePathText.setText(LAST_FILE);
        }
        if(LAST_FOLDER != null) {
            folderPathText.setText(LAST_FOLDER);
        }
        filePathRadio.setSelected(LAST_FILE_SELECTED);
        folderPathRadio.setSelected(!LAST_FILE_SELECTED);
    }

    private void selectCombo(ComboBox<String> combo, String value) {
        if (value != null && combo.getItems().contains(value)){
            combo.getSelectionModel().select(value);
        }
    }

    public void onSelectFileAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save OEM File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));

        File selected = fc.showSaveDialog(codeText.getScene().getWindow());
        if(selected != null) {
            filePathText.setText(selected.getAbsolutePath());
        }
    }

    public void onSelectDirectoryAction(ActionEvent actionEvent) {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Save OEM File to Folder");

        File selected = fc.showDialog(folderPathText.getScene().getWindow());
        if(selected != null) {
            folderPathText.setText(selected.getAbsolutePath());
        }
    }
}
