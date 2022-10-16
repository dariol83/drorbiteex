package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.CelestrakSatellite;
import eu.dariolucia.drorbiteex.data.CelestrakTleOrbit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CelestrakDialog implements Initializable {

    public ComboBox<String> groupCombo;
    public ListView<CelestrakSatellite> satelliteList;
    public ProgressIndicator progressIndicator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        satelliteList.setCellFactory(CheckBoxListCell.forListView(CelestrakSatellite::selectedProperty));
        groupCombo.getItems().addAll("weather", "noaa", "resource", "gps-ops", "galileo", "geo");
    }

    private List<CelestrakTleOrbit> getResult() {
        List<CelestrakTleOrbit> orbits = new LinkedList<>();
        for(CelestrakSatellite cs : satelliteList.getItems()) {
            if(cs.selectedProperty().get()) {
                CelestrakTleOrbit orb = new CelestrakTleOrbit();
                orb.setName(cs.getName());
                orb.setCode(cs.getName());
                orb.setGroup(cs.getGroup());
                orb.setVisible(true);
                orb.setTle(cs.getTle());
                orb.setColor(Color.CYAN.toString()); // TODO: random color
                orbits.add(orb);
            }
        }
        return orbits;
    }

    public void onGroupSelection(ActionEvent actionEvent) {
        String group = this.groupCombo.getSelectionModel().getSelectedItem();
        if(group != null) {
            satelliteList.setDisable(true);
            progressIndicator.setVisible(true);
            Main.runLater(() -> {
                List<CelestrakSatellite> sats = CelestrakSatellite.retrieveSpacecraftList(group);
                Platform.runLater(() -> {
                    if(sats != null) {
                        satelliteList.getItems().clear();
                        satelliteList.getItems().addAll(sats);
                        satelliteList.refresh();
                    }
                    satelliteList.setDisable(false);
                    progressIndicator.setVisible(false);
                });
            });
        }
    }

    public static List<CelestrakTleOrbit> openDialog(Window owner) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Celestrak Orbit Selection");
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = TleOrbitDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/CelestrakDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CelestrakDialog controller = loader.getController();

            d.getDialogPane().setContent(root);
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
