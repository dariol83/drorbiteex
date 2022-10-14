package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.GroundStation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class GroundStationDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextArea descriptionTextArea;
    public TextField latitudeText;
    public TextField longitudeText;
    public ColorPicker colorPicker;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        latitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        longitudeText.textProperty().addListener((prop, oldVal, newVal) -> validate());

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
            Double.parseDouble(latitudeText.getText());
            Double.parseDouble(longitudeText.getText());
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
        descriptionTextArea.setText(gs.getDescription());
        latitudeText.setText(String.valueOf(gs.getLatitude()));
        longitudeText.setText(String.valueOf(gs.getLongitude()));
        colorPicker.setValue(Color.valueOf(gs.getColor()));
    }

    public GroundStation getResult() {
        GroundStation gs = new GroundStation();
        gs.setCode(codeText.getText());
        gs.setName(nameText.getText());
        gs.setDescription(descriptionTextArea.getText());
        gs.setLatitude(Double.parseDouble(latitudeText.getText()));
        gs.setLongitude(Double.parseDouble(longitudeText.getText()));
        gs.setColor(colorPicker.getValue().toString());
        gs.setVisible(true);
        return gs;
    }

    public static GroundStation openDialog(Window owner) {
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
            GroundStationDialog controller = loader.getController();

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
            GroundStationDialog controller = loader.getController();
            controller.setOriginalGroundStation(gs);

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
}
