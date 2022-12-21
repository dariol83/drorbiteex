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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.schedule.*;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Pair;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ExportScheduleDialog implements Initializable {

    private static StatusEnum lastStatus = StatusEnum.OPERATIONAL;
    private static Date lastStartDate = new Date(); // now
    private static Date lastEndDate = new Date(lastStartDate.getTime() + (7 * 24 * 3600 * 1000)); // 7 days

    private static final List<String> lastSelectedOrbitNames = new LinkedList<>();

    private static final List<Pair<ServiceTypeEnum, FrequencyEnum>> lastSelectedServices = new LinkedList<>();

    private static String lastOrigEntityId = "";

    private static int lastDeltaPeriod = 0;

    private static String lastSelectedExporter = null;
    private static String lastSelectedGenerator = null;

    private static String lastFilePath = "";
    private static String lastFolderPath = "";

    private static boolean lastFileRadio = true;

    public TextField startDateText;
    public TextField startTimeText;
    public TextField endDateText;
    public TextField endTimeText;
    public TextField filePathText;

    public CheckBox service1Check;
    public ComboBox<ServiceTypeEnum> type1Combo;
    public ComboBox<FrequencyEnum> service1Combo;
    public CheckBox service2Check;
    public ComboBox<ServiceTypeEnum> type2Combo;
    public ComboBox<FrequencyEnum> service2Combo;
    public CheckBox service3Check;
    public ComboBox<ServiceTypeEnum> type3Combo;
    public ComboBox<FrequencyEnum> service3Combo;
    public CheckBox service4Check;
    public ComboBox<ServiceTypeEnum> type4Combo;
    public ComboBox<FrequencyEnum> service4Combo;

    public ComboBox<StatusEnum> statusCombo;

    public TextField originatingEntityText;
    public ListView<OrbitWrapper> orbitList;
    public ComboBox<String> exporterCombo;
    public TextField startEndActivityDeltaText;
    public RadioButton filePathRadio;
    public RadioButton folderPathRadio;
    public TextField folderPathText;
    public ComboBox<String> fileGeneratorCombo;
    public Button filePathButton;
    public Button folderPathButton;

    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    private GroundStation groundStation;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        orbitList.setCellFactory(CheckBoxListCell.forListView(OrbitWrapper::selectedProperty));

        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        startDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        folderPathText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        originatingEntityText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startEndActivityDeltaText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathRadio.selectedProperty().addListener((prop, oldVal, newVal) -> validate());
        folderPathRadio.selectedProperty().addListener((prop, oldVal, newVal) -> validate());

        initialiseService(service1Check, type1Combo, service1Combo);
        initialiseService(service2Check, type2Combo, service2Combo);
        initialiseService(service3Check, type3Combo, service3Combo);
        initialiseService(service4Check, type4Combo, service4Combo);

        statusCombo.getItems().addAll(StatusEnum.values());
        statusCombo.getSelectionModel().select(StatusEnum.OPERATIONAL);

        for(String exporter : ScheduleExporterRegistry.instance().getExporters()) {
            exporterCombo.getItems().add(exporter);
        }

        ToggleGroup fileTg = new ToggleGroup();
        filePathRadio.setToggleGroup(fileTg);
        folderPathRadio.setToggleGroup(fileTg);

        folderPathText.disableProperty().bind(folderPathRadio.selectedProperty().not());
        folderPathButton.disableProperty().bind(folderPathRadio.selectedProperty().not());
        fileGeneratorCombo.disableProperty().bind(folderPathRadio.selectedProperty().not());
        filePathText.disableProperty().bind(filePathRadio.selectedProperty().not());
        filePathButton.disableProperty().bind(filePathRadio.selectedProperty().not());

        for(String generator : ScheduleExporterRegistry.instance().getNameGenerators()) {
            fileGeneratorCombo.getItems().add(generator);
        }

        validate();
    }

    private void initialiseService(CheckBox enableCheck, ComboBox<ServiceTypeEnum> typeCombo, ComboBox<FrequencyEnum> serviceCombo) {
        typeCombo.disableProperty().bind(enableCheck.selectedProperty().not());
        serviceCombo.disableProperty().bind(enableCheck.selectedProperty().not());
        typeCombo.getItems().addAll(ServiceTypeEnum.values());
        typeCombo.getSelectionModel().select(0);
        serviceCombo.getItems().addAll(FrequencyEnum.values());
        serviceCombo.getSelectionModel().select(0);
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
            if(filePathRadio.isSelected() && filePathText.getText().isBlank()) {
                throw new IllegalStateException("File is not selected");
            }
            if(folderPathRadio.isSelected() && folderPathText.getText().isBlank()) {
                throw new IllegalStateException("Folder is not selected");
            }
            if(originatingEntityText.getText().isBlank()) {
                throw new IllegalStateException("Originating entity is blank");
            }

            Integer.parseInt(startEndActivityDeltaText.getText());

            getDate(startDateText, startTimeText);
            getDate(endDateText, endTimeText);

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public ScheduleGenerationRequest getResult() {
        try {
            Date start = getDate(startDateText, startTimeText);
            Date end = getDate(endDateText, endTimeText);
            lastStartDate = start;
            lastEndDate = end;
            List<Orbit> orbits = orbitList.getItems().stream().filter(OrbitWrapper::isSelected).map(OrbitWrapper::getOrbit).collect(Collectors.toList());
            lastSelectedOrbitNames.clear();
            lastSelectedOrbitNames.addAll(orbits.stream().map(Orbit::getName).collect(Collectors.toList()));
            lastDeltaPeriod = Integer.parseInt(startEndActivityDeltaText.getText());
            lastFilePath = filePathText.getText();
            lastOrigEntityId = originatingEntityText.getText();
            lastStatus = statusCombo.getValue();
            lastSelectedExporter = exporterCombo.getValue();
            lastSelectedGenerator = fileGeneratorCombo.getValue();
            lastFolderPath = folderPathText.getText();
            lastFileRadio = filePathRadio.isSelected();

            return new ScheduleGenerationRequest(this.groundStation, orbits, start, end, originatingEntityText.getText(), statusCombo.getValue(),
                    buildServiceRequests(), exporterCombo.getValue(), Integer.parseInt(startEndActivityDeltaText.getText()),
                    filePathRadio.isSelected() ? filePathText.getText() : null,
                    folderPathRadio.isSelected() ? folderPathText.getText() : null,
                    fileGeneratorCombo.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ServiceInfoRequest> buildServiceRequests() {
        lastSelectedServices.clear();
        List<ServiceInfoRequest> reqs = new LinkedList<>();
        if(service1Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type1Combo.getValue(), service1Combo.getValue()));
            lastSelectedServices.add(new Pair<>(type1Combo.getValue(), service1Combo.getValue()));
        }
        if(service2Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type2Combo.getValue(), service2Combo.getValue()));
            lastSelectedServices.add(new Pair<>(type2Combo.getValue(), service2Combo.getValue()));
        }
        if(service3Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type3Combo.getValue(), service3Combo.getValue()));
            lastSelectedServices.add(new Pair<>(type3Combo.getValue(), service3Combo.getValue()));
        }
        if(service4Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type4Combo.getValue(), service4Combo.getValue()));
            lastSelectedServices.add(new Pair<>(type4Combo.getValue(), service4Combo.getValue()));
        }
        return reqs;
    }

    private Date getDate(TextField dateText, TextField timeText) throws ParseException {
        return dateTimeFormatter.parse(dateText.getText() + " " + timeText.getText());
    }

    public static ScheduleGenerationRequest openDialog(Window owner, GroundStation gs, List<Orbit> orbits) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Export CCSDS Simple Schedule for " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = ExportScheduleDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/ExportScheduleDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            ExportScheduleDialog controller = loader.getController();
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
        this.originatingEntityText.setText(lastOrigEntityId);
        if(lastFileRadio) {
            this.filePathRadio.setSelected(true);
        } else {
            this.folderPathRadio.setSelected(true);
        }
        this.filePathText.setText(lastFilePath);
        this.folderPathText.setText(lastFolderPath);

        this.statusCombo.setValue(lastStatus);
        this.startEndActivityDeltaText.setText(String.valueOf(lastDeltaPeriod));
        for(Orbit o : orbits) {
            OrbitWrapper ow = new OrbitWrapper(o);
            if(lastSelectedOrbitNames.contains(ow.getOrbit().getName())) {
                ow.selectedProperty().set(true);
            }
            this.orbitList.getItems().add(ow);
        }
        if(lastSelectedExporter != null) {
            this.exporterCombo.setValue(lastSelectedExporter);
        } else {
            this.exporterCombo.getSelectionModel().select(0);
        }
        if(lastSelectedGenerator != null) {
            this.fileGeneratorCombo.setValue(lastSelectedGenerator);
        } else {
            this.fileGeneratorCombo.getSelectionModel().select(0);
        }

        CheckBox[] selection = new CheckBox[] {service1Check, service2Check, service3Check, service4Check};
        @SuppressWarnings("unchecked") // Elements are hardcoded, exact type is provided
        ComboBox<FrequencyEnum>[] services = new ComboBox[] {service1Combo, service2Combo, service3Combo, service4Combo};
        @SuppressWarnings("unchecked") // Elements are hardcoded, exact type is provided
        ComboBox<ServiceTypeEnum>[] frequencies = new ComboBox[] {type1Combo, type2Combo, type3Combo, type4Combo};
        int i = 0;
        for(Pair<ServiceTypeEnum, FrequencyEnum> p : lastSelectedServices) {
            services[i].getSelectionModel().select(p.getValue());
            frequencies[i].getSelectionModel().select(p.getKey());
            selection[i].setSelected(true);
            ++i;
        }

        validate();
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

    public void onSelectFileAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CCSDS Simple Schedule File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Schedule file","*.xml"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Schedule file","*.xml"));

        File selected = fc.showSaveDialog(filePathText.getScene().getWindow());
        if(selected != null) {
            filePathText.setText(selected.getAbsolutePath());
        }
    }

    public void onSelectDirectoryAction(ActionEvent actionEvent) {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Save CCSDS Simple Schedule File to Folder");

        File selected = fc.showDialog(folderPathText.getScene().getWindow());
        if(selected != null) {
            folderPathText.setText(selected.getAbsolutePath());
        }
    }

    private static class OrbitWrapper {
        private final Orbit orbit;
        private final SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

        public OrbitWrapper(Orbit orbit) {
            this.orbit = orbit;
        }

        public Orbit getOrbit() {
            return orbit;
        }

        public boolean isSelected() {
            return selectedProperty.get();
        }

        public SimpleBooleanProperty selectedProperty() {
            return selectedProperty;
        }

        @Override
        public String toString() {
            return orbit.getName();
        }
    }
}
