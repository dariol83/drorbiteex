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

import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.util.*;
import java.util.stream.Collectors;

public class CelestrakDialog implements Initializable {

    public ComboBox<String> groupCombo;
    public ListView<CelestrakTleData> satelliteList;
    public ProgressIndicator progressIndicator;
    public TextField filterText;

    private FilteredList<CelestrakTleData> filteredList;
    private ObservableList<CelestrakTleData> dataList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        satelliteList.setCellFactory(CheckBoxListCell.forListView(CelestrakTleData::selectedProperty));
        dataList = FXCollections.observableList(new LinkedList<>());
        filteredList = new FilteredList<>(dataList, s -> true);
        satelliteList.setItems(filteredList);
        groupCombo.getItems().addAll(CelestrakTleData.CELESTRAK_GROUPS);
        filterText.textProperty().addListener(observable -> {
            String filter = filterText.getText();
            if(filter == null || filter.length() == 0) {
                filteredList.setPredicate(s -> true);
            }
            else {
                filteredList.setPredicate(s -> s.getName().contains(filter));
            }
        });
    }

    private List<Orbit> getResult() {
        return satelliteList.getItems().stream()
                .filter(o -> o.selectedProperty().get())
                .map(cs -> new Orbit(UUID.randomUUID(), cs.getName(), cs.getName(), randomColor(),true, new CelestrakTleOrbitModel(cs.getGroup(), cs.getName(), cs.getTle())))
                .collect(Collectors.toList());
    }

    private String randomColor() {
        return new Color(Math.random(), Math.random(), Math.random(), 1.0).toString();
    }

    public void onGroupSelection(ActionEvent actionEvent) {
        String group = this.groupCombo.getSelectionModel().getSelectedItem();
        if(group != null) {
            satelliteList.setDisable(true);
            progressIndicator.setVisible(true);
            filterText.setDisable(true);
            BackgroundThread.runLater(() -> {
                List<CelestrakTleData> sats = CelestrakTleData.retrieveSpacecraftList(group);
                Platform.runLater(() -> {
                    if(sats != null) {
                        dataList.clear();
                        dataList.addAll(sats);
                        satelliteList.refresh();
                    }
                    satelliteList.setDisable(false);
                    progressIndicator.setVisible(false);
                    filterText.setDisable(false);
                });
            });
        }
    }

    public static List<Orbit> openDialog(Window owner) {
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
            CssHolder.applyTo(root);
            CelestrakDialog controller = loader.getController();

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

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

    public void onSelectAllAction(ActionEvent actionEvent) {
        satelliteList.getItems().forEach(o -> o.selectedProperty().set(true));
    }

    public void onDeselectAllAction(ActionEvent actionEvent) {
        satelliteList.getItems().forEach(o -> o.selectedProperty().set(false));
    }
}
