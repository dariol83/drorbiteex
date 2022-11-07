package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
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
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

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


    private void setOriginalOrbit(Orbit gs) {
        codeText.setText(gs.getCode());
        nameText.setText(gs.getName());
        colorPicker.setValue(Color.valueOf(gs.getColor()));
        if(gs.getModel() instanceof CelestrakTleOrbitModel) {
            groupText.setText(((CelestrakTleOrbitModel) gs.getModel()).getGroup());
            tleTextArea.setText(((CelestrakTleOrbitModel) gs.getModel()).getTle());
        }
    }

    public Orbit getResult() {
        return new Orbit(UUID.randomUUID(), codeText.getText(), nameText.getText(), colorPicker.getValue().toString(), true, new CelestrakTleOrbitModel(groupText.getText(), tleTextArea.getText()));
    }

    public static Orbit openDialog(Window owner) {
        return openDialog(owner, null);
    }

    public static Orbit openDialog(Window owner, Orbit gs) {
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
        ModelManager.runLater(() -> {
            String newTle = CelestrakTleData.retrieveUpdatedTle(group, name);
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
