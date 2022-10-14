package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.Configuration;
import eu.dariolucia.drorbiteex.data.GroundStation;
import eu.dariolucia.drorbiteex.data.Orbit;
import eu.dariolucia.drorbiteex.data.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

public class Main implements Initializable {

    private static final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + "drorbiteex";
    private static final String DEFAULT_CONFIG_FILE_NAME = "state.xml";
    private static final String DEFAULT_CONFIG_LOCATION = DEFAULT_CONFIG_FOLDER + File.separator + DEFAULT_CONFIG_FILE_NAME;
    private static final String OREKIT_FOLDER_NAME = "orekit-data";

    private static final String CONFIG_FOLDER_LOCATION_KEY = "drorbiteex.config";


    private File configFile;

    public SubScene scene3d;

    private Group group;
    private Group earth;

    // For dragging purposes
    private boolean dragging;
    private double dragXStart;
    private double dragYStart;

    private double initialYangle = 0;
    private double initialXangle = 0;

    private double currentYangle = 0;
    private double currentXangle = 0;

    private int zoomFactor = 0;
    private double zoomDeltaFactor = 0.4;

    // Ground stations
    public ListView<GroundStation> groundStationList;
    private Group groundStationGroup;

    // Orbits
    public ListView<Orbit> orbitList;
    private Group orbitGroup;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Earth sphere object
        earth = Utils.createEarthSphere();
        groundStationGroup = new Group();
        orbitGroup = new Group();
        group = new Group(earth, groundStationGroup, orbitGroup);

        // Handle 3D view
        scene3d.setFill(Color.BLACK);
        scene3d.setRoot(group);
        scene3d.setDepthTest(DepthTest.ENABLE);
        scene3d.setManaged(false);
        scene3d.setCamera(new PerspectiveCamera());
        scene3d.heightProperty().bind(((VBox)scene3d.getParent()).heightProperty());
        scene3d.widthProperty().bind(((VBox)scene3d.getParent()).widthProperty());
        scene3d.heightProperty().addListener((a,b,c) -> group.setTranslateY(c.floatValue()/2));
        scene3d.widthProperty().addListener((a,b,c) -> group.setTranslateX(c.floatValue()/2));
        scene3d.getParent().addEventHandler(ScrollEvent.SCROLL, this::onScrollOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_PRESSED, this::onStartDragOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_RELEASED, this::onEndDragOnScene);

        // Handle ground station list
        groundStationList.setCellFactory(CheckBoxListCell.forListView(GroundStation::visibleProperty));

        // Load old configuration if available
        String configLocation = System.getProperty(CONFIG_FOLDER_LOCATION_KEY);
        File orekitData;
        if(configLocation != null && !configLocation.isBlank()) {
            configFile = new File(configLocation + File.separator + DEFAULT_CONFIG_FILE_NAME);
            orekitData = new File(configLocation + File.separator + OREKIT_FOLDER_NAME);
        } else {
            configFile = new File(DEFAULT_CONFIG_LOCATION);
            orekitData = new File(DEFAULT_CONFIG_FOLDER + File.separator + OREKIT_FOLDER_NAME);
        }
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        if(configFile.exists()) {
            loadConfigFile();
        }
    }

    private void loadConfigFile() {
        try (FileInputStream is = new FileInputStream(this.configFile)) {
            Configuration c = Configuration.load(is);
            for (GroundStation gs : c.getGroundStations()) {
                initialiseGroundStation(gs);
            }
            for(Orbit gs : c.getOrbits()) {
                initialiseOrbit(gs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Ignore
        }
    }

    private void initialiseGroundStation(GroundStation gs) {
        groundStationList.getItems().add(gs);
        Group s = gs.createGraphicItem();
        groundStationGroup.getChildren().add(s);
    }

    private void saveConfigFile() {
        Configuration c = new Configuration();
        c.setGroundStations(new ArrayList<>());
        for(GroundStation gs : groundStationList.getItems()) {
            c.getGroundStations().add(gs);
        }
        c.setOrbits(new ArrayList<>());
        for(Orbit gs : orbitList.getItems()) {
            c.getOrbits().add(gs);
        }
        try {
            if (this.configFile.exists()) {
                this.configFile.delete();
            }
            this.configFile.getParentFile().mkdirs();
            this.configFile.createNewFile();
            FileOutputStream out = new FileOutputStream(this.configFile);
            Configuration.save(c, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onEndDragOnScene(MouseEvent t) {
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = false;
            dragXStart = 0;
            dragYStart = 0;
            initialYangle = currentYangle;
            initialXangle = currentXangle;
            currentYangle = 0;
            currentXangle = 0;
        }
    }

    private void onDragOnScene(MouseEvent t) {
        if(t.getButton() == MouseButton.PRIMARY && dragging) {
            // compute delta
            double deltaX = t.getSceneX() - dragXStart;
            double deltaY = t.getSceneY() - dragYStart;
            // compute new roll and pitch
            currentXangle = initialXangle + deltaX * zoomDeltaFactor;
            currentYangle = initialYangle + deltaY * zoomDeltaFactor;
            // set rotation
            Rotate xRotation = new Rotate(currentXangle, new Point3D(0,-1, 0));
            Point3D yAxis = new Point3D(Math.cos(Math.toRadians(-currentXangle)),0, Math.sin(Math.toRadians(-currentXangle)));
            Rotate yRotation = new Rotate(currentYangle, yAxis);
            Transform result = xRotation.createConcatenation(yRotation);
            group.getTransforms().clear();
            group.getTransforms().add(result);
        }
    }

    private void onStartDragOnScene(MouseEvent t) {
        if(t.getButton() == MouseButton.PRIMARY) {
            dragging = true;
            dragXStart = t.getSceneX();
            dragYStart = t.getSceneY();
        }
    }

    private void onScrollOnScene(ScrollEvent event) {
        if(event.getDeltaY() < 0) {
            zoomFactor += 1;
            group.setTranslateZ(group.getTranslateZ() + 50);
        } else if(event.getDeltaY() > 0) {
            group.setTranslateZ(group.getTranslateZ() - 50);
            zoomFactor -= 1;
        }
        if(zoomFactor >= -2) {
            zoomDeltaFactor = 0.4;
        } else {
            zoomDeltaFactor = 0.1;
        }
    }

    public void onNewGroundStationAction(ActionEvent actionEvent) {
        GroundStation gs = GroundStationDialog.openDialog(scene3d.getParent().getScene().getWindow());
        if(gs != null) {
            initialiseGroundStation(gs);
            saveConfigFile();
        }
    }

    public void onEditGroundStationAction(ActionEvent mouseEvent) {
        editGroundStation();
    }


    public void onDeleteGroundStation(ActionEvent actionEvent) {
        GroundStation gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Ground Station");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to delete ground station " + gs.getName() + "?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                groundStationList.getItems().remove(gs);
                groundStationGroup.getChildren().remove(gs.getGraphicItem());
                gs.dispose();
                saveConfigFile();
                groundStationList.refresh();
            }
        }
    }

    public void onGroundStationSelectionClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            editGroundStation();
        }
    }

    private void editGroundStation() {
        GroundStation originalGs = groundStationList.getSelectionModel().getSelectedItem();
        if(originalGs != null) {
            GroundStation gs = GroundStationDialog.openDialog(scene3d.getParent().getScene().getWindow(), originalGs);
            if (gs != null) {
                originalGs.update(gs, Utils.EARTH_RADIUS);
                saveConfigFile();
                groundStationList.refresh();
            }
        }
    }

    public void onNewOrbitAction(ActionEvent actionEvent) {
        Orbit gs = OrbitDialog.openDialog(scene3d.getParent().getScene().getWindow());
        if(gs != null) {
            initialiseOrbit(gs);
            saveConfigFile();
        }
    }

    private void initialiseOrbit(Orbit gs) {
        orbitList.getItems().add(gs);
        Group s = gs.createGraphicItem();
        orbitGroup.getChildren().add(s);
    }

    public void onDeleteOrbitStation(ActionEvent actionEvent) {
        Orbit gs = orbitList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Orbit");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to delete orbit for " + gs.getName() + "?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                orbitList.getItems().remove(gs);
                orbitGroup.getChildren().remove(gs.getGraphicItem());
                gs.dispose();
                saveConfigFile();
                orbitList.refresh();
            }
        }
    }

    public void onEditOrbitAction(ActionEvent actionEvent) {
    }

    public void onOrbitSelectionClick(MouseEvent mouseEvent) {
    }
}
