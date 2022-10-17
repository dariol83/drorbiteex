package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.CelestrakSatellite;
import eu.dariolucia.drorbiteex.data.CelestrakTleOrbit;
import eu.dariolucia.drorbiteex.data.TleOrbit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CelestrakTleOrbitDialog implements Initializable {
    public TextField codeText;
    public TextField nameText;
    public TextField groupText;
    public TextArea tleTextArea;

    public ColorPicker colorPicker;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);
    public Button tleReloadButton;
    public ProgressIndicator tleProgress;

    private String error;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        codeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        tleTextArea.textProperty().addListener((prop, oldVal, newVal) -> validate());

        validate();
    }

    private void validate() {
        try {
            if(codeText.getText().isBlank()) {
                throw new IllegalStateException("Code field is blank");
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


    private void setOriginalOrbit(CelestrakTleOrbit gs) {
        codeText.setText(gs.getCode());
        groupText.setText(gs.getGroup());
        nameText.setText(gs.getName());
        tleTextArea.setText(gs.getTle());
        colorPicker.setValue(Color.valueOf(gs.getColor()));
    }

    public CelestrakTleOrbit getResult() {
        CelestrakTleOrbit gs = new CelestrakTleOrbit();
        gs.setCode(codeText.getText());
        gs.setName(nameText.getText());
        gs.setGroup(groupText.getText());
        gs.setTle(tleTextArea.getText());
        gs.setColor(colorPicker.getValue().toString());
        gs.setVisible(true);
        return gs;
    }

    public static TleOrbit openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static CelestrakTleOrbit openDialog(Window owner, CelestrakTleOrbit gs) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Celestrak TLE Orbit");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = CelestrakTleOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/CelestrakTleOrbitDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CelestrakTleOrbitDialog controller = loader.getController();
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

    public void onReloadTleAction(ActionEvent actionEvent) {
        tleReloadButton.setDisable(true);
        tleTextArea.setDisable(true);
        tleProgress.setVisible(true);
        final String group = groupText.getText();
        final String name = nameText.getText();
        Main.runLater(() -> {
            String newTle = CelestrakSatellite.retrieveUpdatedTle(group, name);
            Platform.runLater(() -> {
                if(newTle != null) {
                    tleTextArea.setText(newTle);
                }
                tleReloadButton.setDisable(false);
                tleTextArea.setDisable(false);
                tleProgress.setVisible(false);
            });
        });
    }
}
