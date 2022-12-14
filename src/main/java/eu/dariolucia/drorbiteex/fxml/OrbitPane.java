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

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.oem.OemGenerationRequest;
import eu.dariolucia.drorbiteex.model.orbit.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class OrbitPane implements Initializable {

    public ListView<OrbitGraphics> orbitList;

    // Orbit panel
    public OrbitDetailPanel orbitDetailPanelController;
    public ToggleButton satelliteAutotrackButton;
    public Label orbitInfoLabel;
    private Consumer<OrbitGraphics> autotrackSelectionConsumer;

    private ModelManager manager;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        orbitList.setCellFactory(CheckBoxListCell.forListView(OrbitGraphics::visibleProperty));
        orbitList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> updateOrbitPanelSelection(a, b));
        orbitList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public ObservableList<OrbitGraphics> getOrbitGraphics() {
        return orbitList.getItems();
    }

    public void configure(ModelManager manager, Consumer<OrbitGraphics> autotrackSelectionConsumer) {
        this.manager = manager;
        this.autotrackSelectionConsumer = autotrackSelectionConsumer;
    }

    public OrbitGraphics registerNewOrbit(Orbit o) {
        OrbitGraphics graphics = new OrbitGraphics(this.manager, o);
        orbitList.getItems().add(graphics);
        return graphics;
    }


    public Optional<OrbitGraphics> getGraphicsOf(Orbit orbit) {
        return orbitList.getItems().stream().filter(o -> o.getOrbit().equals(orbit)).findFirst();
    }

    public void deregisterOrbit(OrbitGraphics graphics) {
        orbitList.getItems().remove(graphics);
        graphics.dispose();
        orbitList.refresh();
    }


    public void onNewOrbitAction(ActionEvent actionEvent) {
        Orbit gs = TleOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow());
        if(gs != null) {
            BackgroundThread.runLater(() -> manager.getOrbitManager().newOrbit(
                    gs.getCode(), gs.getName(), gs.getColor(), gs.isVisible(), gs.getModel()
            ));
        }
    }

    public void onNewCelestrakOrbitAction(ActionEvent actionEvent) {
        List<Orbit> theNewOrbits = CelestrakDialog.openDialog(orbitList.getParent().getScene().getWindow());
        if(theNewOrbits != null) {
            theNewOrbits.forEach(gs -> BackgroundThread.runLater(() -> manager.getOrbitManager().newOrbit(
                    gs.getCode(), gs.getName(), gs.getColor(), gs.isVisible(), gs.getModel()
            )));
        }
    }

    public void onNewOemOrbitAction(ActionEvent actionEvent) {
        Orbit gs = OemOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow());
        if(gs != null) {
            BackgroundThread.runLater(() -> manager.getOrbitManager().newOrbit(
                    gs.getCode(), gs.getName(), gs.getColor(), gs.isVisible(), gs.getModel()
            ));
        }
    }

    public void onRefreshCelestrakOrbitAction(ActionEvent actionEvent) {
        //
        for(OrbitGraphics ao : orbitList.getItems()) {
            if (ao.getOrbit().getModel() instanceof CelestrakTleOrbitModel) {
                final Orbit orbit = ao.getOrbit();
                final CelestrakTleOrbitModel theOrbit = (CelestrakTleOrbitModel) orbit.getModel();
                BackgroundThread.runLater(() -> {
                    String newTle = CelestrakTleData.retrieveUpdatedTle(theOrbit.getGroup(), orbit.getName());
                    if(newTle != null) {
                        CelestrakTleOrbitModel model = new CelestrakTleOrbitModel(theOrbit.getGroup(), theOrbit.getCelestrakName(), newTle);
                        orbit.update(new Orbit(orbit.getId(), orbit.getCode(), orbit.getName(), orbit.getColor(), orbit.isVisible(), model));
                    }
                });
            }
        }
    }

    public void onDeleteOrbitAction(ActionEvent actionEvent) {
        List<OrbitGraphics> orbits = orbitList.getSelectionModel().getSelectedItems();
        if(orbits != null && !orbits.isEmpty()) {
            boolean confirmed = false;
            if(orbits.size() == 1) {
                // One orbit
                confirmed = DialogUtils.confirm("Delete Orbit", null, "Do you want to delete orbit for " + orbits.get(0).getName() + "?");
            } else {
                // Multiple orbits
                confirmed = DialogUtils.confirm("Delete Orbits", null, "Do you want to delete the selected orbits?");
            }
            if (confirmed) {
                BackgroundThread.runLater(() ->  {
                    for(OrbitGraphics og : orbits) {
                        manager.getOrbitManager().removeOrbit(og.getOrbit().getId());
                    }
                });
            }
        }
    }

    public void onEditOrbitAction(ActionEvent actionEvent) {
        editOrbit();
    }

    public void onOrbitSelectionClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            editOrbit();
        }
    }

    private void editOrbit() {
        OrbitGraphics originalOrbit = orbitList.getSelectionModel().getSelectedItem();
        if(originalOrbit != null) {
            Orbit orbit = originalOrbit.getOrbit();
            if(orbit.getModel() instanceof CelestrakTleOrbitModel) {
                Orbit ob = CelestrakTleOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    BackgroundThread.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            } else if(orbit.getModel()  instanceof TleOrbitModel) {
                Orbit ob = TleOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    BackgroundThread.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            } else if(orbit.getModel()  instanceof OemOrbitModel) {
                Orbit ob = OemOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    BackgroundThread.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            }
        }
    }

    public void onExportOemOrbitAction(ActionEvent actionEvent) {
        OrbitGraphics originalOrbit = orbitList.getSelectionModel().getSelectedItem();
        if(originalOrbit != null) {
            Orbit orbit = originalOrbit.getOrbit();
            OemGenerationRequest oemGenerationRequest = ExportOemOrbitDialog.openDialog(orbitList.getParent().getScene().getWindow(), orbit);
            if(oemGenerationRequest != null) {
                BackgroundThread.runLater(() -> {
                    try {
                        final String finalPath = manager.getOrbitManager().exportOem(oemGenerationRequest);
                        Platform.runLater(() -> DialogUtils.info("OEM Export", "Orbit of " + orbit.getName() + " exported", "OEM file: " + finalPath));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> DialogUtils.alert("OEM Export", "Orbit of " + orbit.getName() + " not exported", "Error: " + e.getMessage()));
                    }
                });
            }
        }
    }

    public void onSettingsOrbitAction(ActionEvent actionEvent) {
        OrbitParameterConfiguration originalProps = this.manager.getOrbitManager().getConfiguration();
        OrbitParameterConfiguration props = OrbitConfigurationDialog.openDialog(orbitList.getParent().getScene().getWindow(), originalProps);
        if(props != null) {
            BackgroundThread.runLater(() -> manager.updateOrbitParameters(props)); // This triggers a full update
        }
    }

    private void updateOrbitPanelSelection(OrbitGraphics old, OrbitGraphics graphics) {
        if(old != null) {
            old.selectedProperty().set(false);
        }
        if(graphics == null) {
            this.orbitDetailPanelController.clear();
            this.orbitInfoLabel.setText("Orbit Information");
        } else {
            this.orbitDetailPanelController.update(graphics.getOrbit());
            graphics.selectedProperty().set(true);
            this.orbitInfoLabel.setText("Orbit Information - " + graphics.getName());
        }
        if(this.satelliteAutotrackButton.isSelected()) {
            if(graphics == null) {
                this.satelliteAutotrackButton.setSelected(false);
            }
            this.autotrackSelectionConsumer.accept(graphics);
        }
    }

    public void onActivateSatelliteTrackingAction(ActionEvent actionEvent) {
        if(this.satelliteAutotrackButton.isSelected()) {
            OrbitGraphics selectedOrbit = this.orbitList.getSelectionModel().getSelectedItem();
            if(selectedOrbit == null) {
                // Deselect, no satellite selected
                this.satelliteAutotrackButton.setSelected(false);
                this.autotrackSelectionConsumer.accept(null);
            } else {
                // Get the satellite and activate the satellite tracking on the 2D view
                this.autotrackSelectionConsumer.accept(selectedOrbit);
            }
        } else {
            this.autotrackSelectionConsumer.accept(null);
        }
    }

    public void refreshOrbitList() {
        orbitList.refresh();
    }

    public void addSelectionSubscriber(Consumer<OrbitGraphics> selectionListener) {
        this.orbitList.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> selectionListener.accept(c));
    }

    public void updateSpacecraftPosition(Orbit orbit, SpacecraftPosition currentPosition) {
        this.orbitDetailPanelController.updatePosition(orbit, currentPosition);
    }
}
