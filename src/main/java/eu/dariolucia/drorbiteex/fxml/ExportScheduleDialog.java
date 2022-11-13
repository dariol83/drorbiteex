package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.station.FrequencyEnum;
import eu.dariolucia.drorbiteex.model.station.StatusEnum;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.frames.Frame;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class ExportScheduleDialog implements Initializable {

    private static int LAST_PERIOD = 30;
    private static long LAST_TIME_DIFFERENCE = 7 * 24 * 3600 * 1000L;

    public TextField startDateText;
    public TextField startTimeText;
    public TextField endDateText;
    public TextField endTimeText;
    public TextField filePathText;
    public CheckBox telemetryCheck;
    public ComboBox<FrequencyEnum> telemetryCombo;
    public CheckBox telecommandCheck;
    public ComboBox<FrequencyEnum> telecommandCombo;
    public CheckBox rangingCheck;
    public ComboBox<FrequencyEnum> rangingCombo;
    public ComboBox<StatusEnum> statusCombo;
    public TextField userText;
    public TextField originatingEntityText;
    public ListView<Orbit> orbitList;

    private boolean isTle = false;

    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        startDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathText.textProperty().addListener((prop, oldVal, newVal) -> validate());


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
            if(filePathText.getText().isBlank()) {
                throw new IllegalStateException("File is not selected");
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

    public ExportScheduleResult getResult() {
        try {
            Date start = getDate(startDateText, startTimeText);
            Date end = getDate(endDateText, endTimeText);
            LAST_TIME_DIFFERENCE = end.getTime() - start.getTime();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Date getDate(TextField dateText, TextField timeText) throws ParseException {
        return dateTimeFormatter.parse(dateText.getText() + " " + timeText.getText());
    }

    public static ExportScheduleResult openDialog(Window owner, Orbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Export OEM of " + gs.getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = ExportScheduleDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/ExportOemOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            ExportScheduleDialog controller = loader.getController();
            controller.initialise(gs);

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

    private void initialise(Orbit gs) {
        Date refDate = new Date();
        this.startDateText.setText(toDateText(refDate));
        this.startTimeText.setText(toTimeText(refDate));
        this.endDateText.setText(toDateText(new Date(refDate.getTime() + LAST_TIME_DIFFERENCE)));
        this.endTimeText.setText(toTimeText(new Date()));
        this.isTle = gs.getModel() instanceof TleOrbitModel;
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

    public void onSelectScheduleAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Schedule File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Schedule file","*.xml"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Schedule file","*.xml"));

        File selected = fc.showSaveDialog(startDateText.getScene().getWindow());
        if(selected != null) {
            filePathText.setText(selected.getAbsolutePath());
        }
    }

    public static class ExportScheduleResult {
        private final String code;
        private final String name;
        private final Date startTime;
        private final Date endTime;
        private final int periodSeconds;
        private final String file;

        private final Frame frame;

        private final FileFormat format;

        public ExportScheduleResult(String code, String name, Date startTime, Date endTime, int periodSeconds, String file, Frame frame, FileFormat format) {
            this.code = code;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
            this.periodSeconds = periodSeconds;
            this.file = file;
            this.frame = frame;
            this.format = format;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public int getPeriodSeconds() {
            return periodSeconds;
        }

        public String getFile() {
            return file;
        }

        public FileFormat getFormat() {
            return format;
        }

        public Frame getFrame() {
            return frame;
        }
    }
}
