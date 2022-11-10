package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
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
import org.orekit.frames.FramesFactory;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class ExportOemOrbitDialog implements Initializable {

    private static int LAST_PERIOD = 30;
    private static long LAST_TIME_DIFFERENCE = 7 * 24 * 3600 * 1000L;

    public TextField codeText;
    public TextField nameText;
    public TextField startDateText;
    public TextField startTimeText;
    public TextField endDateText;
    public TextField endTimeText;
    public TextField periodText;
    public TextField filePathText;
    public ComboBox<String> frameCombo;
    public ComboBox<String> formatCombo;

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

        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDateText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        filePathText.textProperty().addListener((prop, oldVal, newVal) -> validate());

        formatCombo.getSelectionModel().select(0);
        frameCombo.getSelectionModel().select(0);

        periodText.setText(String.valueOf(LAST_PERIOD));

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
            if(!isTle && frameCombo.getSelectionModel().getSelectedItem().equals("TEME")) {
                throw new IllegalStateException("TEME reference frame can only be used with TLE orbits");
            }

            getDate(startDateText, startTimeText);
            getDate(endDateText, endTimeText);

            Integer.parseInt(periodText.getText());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    public ExportOemResult getResult() {
        try {
            LAST_PERIOD = Integer.parseInt(periodText.getText());
            Date start = getDate(startDateText, startTimeText);
            Date end = getDate(endDateText, endTimeText);
            LAST_TIME_DIFFERENCE = end.getTime() - start.getTime();
            Frame frame = getFrame();
            FileFormat format = getFormat();
            return new ExportOemResult(codeText.getText(), nameText.getText(), start, end, Integer.parseInt(periodText.getText()), filePathText.getText(), frame, format);
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

    private Date getDate(TextField dateText, TextField timeText) throws ParseException {
        return dateTimeFormatter.parse(dateText.getText() + " " + timeText.getText());
    }

    public static ExportOemResult openDialog(Window owner, Orbit gs) {
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
            ExportOemOrbitDialog controller = loader.getController();
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
        this.nameText.setText(gs.getName());
        this.codeText.setText(gs.getCode());
        Date refDate = new Date();
        this.startDateText.setText(toDateText(refDate));
        this.startTimeText.setText(toTimeText(refDate));
        this.endDateText.setText(toDateText(new Date(refDate.getTime() + LAST_TIME_DIFFERENCE)));
        this.endTimeText.setText(toTimeText(new Date()));
        this.periodText.setText(String.valueOf(LAST_PERIOD));
        this.isTle = gs.getModel() instanceof TleOrbitModel;
    }

    private String toTimeText(Date date) {
        return timeFormatter.format(date);
    }

    private String toDateText(Date date) {
        return dateFormatter.format(date);
    }

    public void onSelectOemAction(ActionEvent actionEvent) {
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

    public static class ExportOemResult {
        private final String code;
        private final String name;
        private final Date startTime;
        private final Date endTime;
        private final int periodSeconds;
        private final String file;

        private final Frame frame;

        private final FileFormat format;

        public ExportOemResult(String code, String name, Date startTime, Date endTime, int periodSeconds, String file, Frame frame, FileFormat format) {
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
