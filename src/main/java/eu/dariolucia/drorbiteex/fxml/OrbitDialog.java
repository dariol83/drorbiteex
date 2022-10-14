package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.Orbit;
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

public class OrbitDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextArea tleTextArea;

    public ColorPicker colorPicker;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        nameText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        tleTextArea.textProperty().addListener((prop, oldVal, newVal) -> validate());

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
            if(tleTextArea.getText().isBlank()) {
                throw new IllegalStateException("TLE field is blank");
            }
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
        tleTextArea.setText(gs.getTle());
        colorPicker.setValue(Color.valueOf(gs.getColor()));
    }

    public Orbit getResult() {
        Orbit gs = new Orbit();
        gs.setCode(codeText.getText());
        gs.setName(nameText.getText());
        gs.setTle(tleTextArea.getText());
        gs.setColor(colorPicker.getValue().toString());
        gs.setVisible(true);
        return gs;
    }

    public static Orbit openDialog(Window owner) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Orbit");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/OrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            OrbitDialog controller = loader.getController();

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

    public static Orbit openDialog(Window owner, Orbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Ground Station");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = OrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/GroundStationDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            OrbitDialog controller = loader.getController();
            controller.setOriginalOrbit(gs);

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
