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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ExportScheduleDialog implements Initializable {

    private static StatusEnum LAST_STATUS = StatusEnum.OPERATIONAL;
    private static Date LAST_START_DATE = new Date(); // now
    private static Date LAST_END_DATE = new Date(LAST_START_DATE.getTime() + (7 * 24 * 3600 * 1000)); // 7 days

    private static List<String> LAST_SELECTED_ORBIT_NAMES = new LinkedList<>();

    private static String LAST_ORIG_ENTITY_ID = "";

    private static int LAST_DELTA_PERIOD = 0;

    private static String LAST_SELECTED_EXPORTER = null;

    private static String LAST_FILE_PATH = "";

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
        originatingEntityText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startEndActivityDeltaText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        initialiseService(service1Check, type1Combo, service1Combo);
        initialiseService(service2Check, type2Combo, service2Combo);
        initialiseService(service3Check, type3Combo, service3Combo);
        initialiseService(service4Check, type4Combo, service4Combo);

        statusCombo.getItems().addAll(StatusEnum.values());
        statusCombo.getSelectionModel().select(StatusEnum.OPERATIONAL);

        for(String exporter : ScheduleExporterRegistry.instance().getExporters()) {
            exporterCombo.getItems().add(exporter);
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
            if(filePathText.getText().isBlank()) {
                throw new IllegalStateException("File is not selected");
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
            LAST_START_DATE = start;
            LAST_END_DATE = end;
            List<Orbit> orbits = orbitList.getItems().stream().filter(OrbitWrapper::isSelected).map(OrbitWrapper::getOrbit).collect(Collectors.toList());
            LAST_SELECTED_ORBIT_NAMES.clear();
            LAST_SELECTED_ORBIT_NAMES.addAll(orbits.stream().map(Orbit::getName).collect(Collectors.toList()));
            LAST_DELTA_PERIOD = Integer.parseInt(startEndActivityDeltaText.getText());
            LAST_FILE_PATH = filePathText.getText();
            LAST_ORIG_ENTITY_ID = originatingEntityText.getText();
            LAST_STATUS = statusCombo.getValue();

            return new ScheduleGenerationRequest(this.groundStation, orbits, start, end, originatingEntityText.getText(), statusCombo.getValue(),
                    buildServiceRequests(), exporterCombo.getValue(), Integer.parseInt(startEndActivityDeltaText.getText()), filePathText.getText());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ServiceInfoRequest> buildServiceRequests() {
        List<ServiceInfoRequest> reqs = new LinkedList<>();
        if(service1Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type1Combo.getValue(), service1Combo.getValue()));
        }
        if(service2Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type2Combo.getValue(), service2Combo.getValue()));
        }
        if(service3Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type3Combo.getValue(), service3Combo.getValue()));
        }
        if(service4Check.isSelected()) {
            reqs.add(new ServiceInfoRequest(type4Combo.getValue(), service4Combo.getValue()));
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
        this.startDateText.setText(toDateText(LAST_START_DATE));
        this.startTimeText.setText(toTimeText(LAST_START_DATE));
        this.endDateText.setText(toDateText(LAST_END_DATE));
        this.endTimeText.setText(toTimeText(LAST_END_DATE));
        this.originatingEntityText.setText(LAST_ORIG_ENTITY_ID);
        this.filePathText.setText(LAST_FILE_PATH);
        this.statusCombo.setValue(LAST_STATUS);
        this.startEndActivityDeltaText.setText(String.valueOf(LAST_DELTA_PERIOD));
        for(Orbit o : orbits) {
            OrbitWrapper ow = new OrbitWrapper(o);
            if(LAST_SELECTED_ORBIT_NAMES.contains(ow.getOrbit().getName())) {
                ow.selectedProperty().set(true);
            }
            this.orbitList.getItems().add(ow);
        }
        if(LAST_SELECTED_EXPORTER != null) {
            this.exporterCombo.setValue(LAST_SELECTED_EXPORTER);
        } else {
            this.exporterCombo.getSelectionModel().select(0);
        }
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

    public void onSelectScheduleAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CCSDS Simple Schedule File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Schedule file","*.xml"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Schedule file","*.xml"));

        File selected = fc.showSaveDialog(startDateText.getScene().getWindow());
        if(selected != null) {
            filePathText.setText(selected.getAbsolutePath());
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
