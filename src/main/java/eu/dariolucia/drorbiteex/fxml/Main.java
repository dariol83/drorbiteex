package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.*;
import eu.dariolucia.drorbiteex.model.schedule.ScheduleGenerationRequest;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.util.StringConverter;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Main implements Initializable, IOrbitListener, IGroundStationListener {

    private static final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + "drorbiteex";
    private static final String DEFAULT_ORBIT_CONFIG_FILE_NAME = "orbits.xml";
    private static final String DEFAULT_ORBIT_CONFIG_LOCATION = DEFAULT_CONFIG_FOLDER + File.separator + DEFAULT_ORBIT_CONFIG_FILE_NAME;
    private static final String DEFAULT_GS_CONFIG_FILE_NAME = "groundstations.xml";
    private static final String DEFAULT_GS_CONFIG_LOCATION = DEFAULT_CONFIG_FOLDER + File.separator + DEFAULT_GS_CONFIG_FILE_NAME;

    private static final String CONFIG_FOLDER_LOCATION_KEY = "drorbiteex.config";
    private static final String OREKIT_FOLDER_NAME = "orekit-data";
    private static final String NO_GROUND_TRACK = "             ";

    public SubScene scene3d;
    public Label processingLabel;

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
    public ListView<GroundStationGraphics> groundStationList;
    private Group groundStationGroup;

    // Orbits
    public ListView<OrbitGraphics> orbitList;
    private Group orbitGroup;

    // Time tracker
    public ToggleButton timerTrackingButton;
    public Label currentTimeLabel;
    private final Timer tracker = new Timer();
    private TimerTask timerTask = null;

    // 2D scene (minimap)
    public Canvas scene2d;
    private Image scene2dImage;
    public ToggleButton minimapButton;

    // Pass table
    public TableView<VisibilityWindow> passTable;
    public TableColumn<VisibilityWindow, String> satelliteColumn;
    public TableColumn<VisibilityWindow, String> orbitColumn;
    public TableColumn<VisibilityWindow, String> aosColumn;
    public TableColumn<VisibilityWindow, String> losColumn;

    // Ground track combo selection
    public ComboBox<Object> groundTrackCombo;
    private ChangeListener<Boolean> visibilityUpdateListener = (observableValue, aBoolean, t1) -> update2Dscene();

    // Polar plot
    public PolarPlot polarPlotController;
    public ProgressIndicator polarPlotProgress;

    // Orbit panel
    public OrbitPanel orbitPanelController;

    private ModelManager manager;

    private boolean orbitUpdateInProgress = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load old configuration if available
        String configLocation = System.getProperty(CONFIG_FOLDER_LOCATION_KEY);
        File orekitData;
        String orbitFile;
        String gsFile;
        if(configLocation != null && !configLocation.isBlank()) {
            orbitFile = configLocation + File.separator + DEFAULT_ORBIT_CONFIG_FILE_NAME;
            gsFile = configLocation + File.separator + DEFAULT_GS_CONFIG_FILE_NAME;
            orekitData = new File(configLocation + File.separator + OREKIT_FOLDER_NAME);
        } else {
            orbitFile = DEFAULT_ORBIT_CONFIG_LOCATION;
            gsFile = DEFAULT_GS_CONFIG_LOCATION;
            orekitData = new File(DEFAULT_CONFIG_FOLDER + File.separator + OREKIT_FOLDER_NAME);
        }
        // Orekit initialisation
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // Earth sphere object
        earth = DrawingUtils.createEarthSphere();
        groundStationGroup = new Group();
        orbitGroup = new Group();
        group = new Group(earth, groundStationGroup, orbitGroup);

        // Handle 3D view
        scene3d.setFill(Color.BLACK);
        scene3d.setRoot(group);
        scene3d.setDepthTest(DepthTest.ENABLE);
        scene3d.setManaged(false);
        scene3d.setCamera(new PerspectiveCamera());
        scene3d.heightProperty().bind(((VBox)scene3d.getParent().getParent()).heightProperty());
        scene3d.widthProperty().bind(((VBox)scene3d.getParent().getParent()).widthProperty());
        scene3d.heightProperty().addListener((a,b,c) -> group.setTranslateY(c.floatValue()/2));
        scene3d.widthProperty().addListener((a,b,c) -> group.setTranslateX(c.floatValue()/2));
        scene3d.getParent().addEventHandler(ScrollEvent.SCROLL, this::onScrollOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_PRESSED, this::onStartDragOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragOnScene);
        scene3d.getParent().addEventHandler(MouseEvent.MOUSE_RELEASED, this::onEndDragOnScene);

        // Handle ground station and orbit lists
        groundStationList.setCellFactory(CheckBoxListCell.forListView(GroundStationGraphics::visibleProperty));
        orbitList.setCellFactory(CheckBoxListCell.forListView(OrbitGraphics::visibleProperty));

        // Handle ground track list
        groundTrackCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Object o) {
                if (o.equals(NO_GROUND_TRACK)) {
                    return NO_GROUND_TRACK;
                } else if (o instanceof OrbitGraphics) {
                    return ((OrbitGraphics) o).getName();
                } else {
                    throw new IllegalStateException("Wrong conversion object: " + o);
                }
            }

            @Override
            public Object fromString(String s) {
                if (s.equals(NO_GROUND_TRACK)) {
                    return NO_GROUND_TRACK;
                } else {
                    for (OrbitGraphics ao : orbitList.getItems()) {
                        if (ao.getName().equals(s)) {
                            return ao;
                        }
                    }
                    throw new IllegalStateException("Wrong conversion string: " + s);
                }
            }
        });
        groundTrackCombo.getItems().add(NO_GROUND_TRACK);
        orbitList.getItems().addListener((ListChangeListener<OrbitGraphics>) c -> {
            while (c.next()) {
                for (OrbitGraphics remitem : c.getRemoved()) {
                    groundTrackCombo.getItems().remove(remitem);
                }
                for (OrbitGraphics additem : c.getAddedSubList()) {
                    groundTrackCombo.getItems().add(additem);
                }
            }
        });
        groundTrackCombo.getSelectionModel().select(0);

        // Polar plot color configuration
        polarPlotController.setForegroundColor(Color.LIMEGREEN);
        polarPlotController.setBackgroundColor(Color.BLACK);

        // Update 2D view
        this.scene2dImage = new Image(this.getClass().getResourceAsStream("/images/earth.jpg"));
        update2Dscene();
        this.scene2d.visibleProperty().bind(this.minimapButton.selectedProperty());

        // Configure pass table
        satelliteColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getOrbit().getName()));
        orbitColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(String.valueOf(o.getValue().getOrbitNumber())));
        aosColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getAosString()));
        losColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().getLosString()));
        groundStationList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> updatePassTableSelection(b));
        passTable.getSelectionModel().selectedItemProperty().addListener((a,b,c) -> updatePolarPlotSelection(c));
        orbitList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> updateOrbitPanelSelection(b));
        // Create model manager
        this.manager = new ModelManager(orbitFile, gsFile);
        this.manager.getOrbitManager().addListener(this);
        this.manager.getGroundStationManager().addListener(this);

        // Create graphics objects
        for(Orbit o : this.manager.getOrbitManager().getOrbits().values()) {
            registerNewOrbit(o);
        }

        for(GroundStation gs : this.manager.getGroundStationManager().getGroundStations().values()) {
            registerNewGroundStation(gs);
        }

        // Redraw stuff on the 2D scene
        update2Dscene();

        // Activate satellite tracking
        timerTrackingButton.setSelected(true);
        onActivateTrackingAction(null);
    }

    private void registerNewGroundStation(GroundStation gs) {
        GroundStationGraphics graphics = new GroundStationGraphics(this.manager, gs);
        groundStationList.getItems().add(graphics);
        Group s = graphics.createGraphicItem();
        groundStationGroup.getChildren().add(s);
        graphics.visibleProperty().addListener(this.visibilityUpdateListener);
        update2Dscene();
    }

    private void deregisterGroundStation(GroundStation gs) {
        Optional<GroundStationGraphics> first = groundStationList.getItems().stream().filter(o -> o.getGroundStation().equals(gs)).findFirst();
        if(first.isPresent()) {
            GroundStationGraphics graphics = first.get();
            groundStationList.getItems().remove(graphics);
            groundStationGroup.getChildren().remove(graphics.getGraphicItem());
            graphics.visibleProperty().removeListener(this.visibilityUpdateListener);
            graphics.dispose();
            groundStationList.refresh();
            update2Dscene();
        }
    }

    private void registerNewOrbit(Orbit o) {
        OrbitGraphics graphics = new OrbitGraphics(this.manager, o);
        orbitList.getItems().add(graphics);
        Group s = graphics.createGraphicItem();
        orbitGroup.getChildren().add(s);
        graphics.visibleProperty().addListener(this.visibilityUpdateListener);
        update2Dscene();
    }

    private void deregisterOrbit(Orbit orbit) {
        Optional<OrbitGraphics> first = orbitList.getItems().stream().filter(o -> o.getOrbit().equals(orbit)).findFirst();
        if(first.isPresent()) {
            OrbitGraphics graphics = first.get();
            orbitList.getItems().remove(graphics);
            orbitGroup.getChildren().remove(graphics.getGraphicItem());
            graphics.visibleProperty().removeListener(this.visibilityUpdateListener);
            graphics.dispose();
            orbitList.refresh();
            update2Dscene();
            updatePassTableSelection(groundStationList.getSelectionModel().getSelectedItem());
        }
    }

    private void updatePassTableSelection(GroundStationGraphics b) {
        // If there is already a pass selected, remember it
        VisibilityWindow selected = passTable.getSelectionModel().getSelectedItem();
        passTable.getItems().clear();
        if(b != null) {
            Map<Orbit, List<VisibilityWindow>> windows = b.getGroundStation().getAllVisibilityWindows();
            List<VisibilityWindow> vw = new LinkedList<>();
            windows.values().forEach(vw::addAll);
            Collections.sort(vw);
            passTable.getItems().addAll(vw);
            // If there was a selection, re-select it
            if(selected != null && selected.getStation().equals(b.getGroundStation())) {
                // Look for the visibility and select it
                for(VisibilityWindow window : passTable.getItems()) {
                    if(window.getOrbit().equals(selected.getOrbit()) && window.getOrbitNumber() == selected.getOrbitNumber()) {
                        // Found
                        passTable.getSelectionModel().select(window);
                        break;
                    }
                }
            }
        }
    }

    private void update2Dscene() {
        System.out.println("Updating 2D scene");
        // Handle 2D view
        GraphicsContext gc = scene2d.getGraphicsContext2D();
        gc.drawImage(this.scene2dImage, 0, 0, scene2d.getWidth(), scene2d.getHeight());
        for(GroundStationGraphics gs : groundStationList.getItems()) {
            gs.draw(gc, getGroundTrackSelection(), scene2d.getWidth(), scene2d.getHeight());
        }
        for(OrbitGraphics gs : orbitList.getItems()) {
            gs.draw(gc, scene2d.getWidth(), scene2d.getHeight());
        }
        // Done
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
        // Register the ground station to the manager: the callbacks will do the rest
        if(gs != null) {
            ModelManager.runLater(() -> manager.getGroundStationManager().newGroundStation(
                    gs.getCode(), gs.getName(), gs.getSite(), gs.getDescription(), gs.getColor(), gs.isVisible(), gs.getLatitude(), gs.getLongitude(), gs.getHeight()
            ));
        }
    }

    public void onEditGroundStationAction(ActionEvent mouseEvent) {
        editGroundStation();
    }

    public void onDeleteGroundStationAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Ground Station");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to delete ground station " + gs.getName() + "?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK){
                ModelManager.runLater(() -> manager.getGroundStationManager().removeGroundStation(gs.getGroundStation().getId()));
            }
        }
    }

    public void onGroundStationSelectionClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            editGroundStation();
        }
    }

    private void editGroundStation() {
        GroundStationGraphics originalGs = groundStationList.getSelectionModel().getSelectedItem();
        if(originalGs != null) {
            GroundStation gs = GroundStationDialog.openDialog(scene3d.getParent().getScene().getWindow(), originalGs.getGroundStation());
            if (gs != null) {
                // Go via the manager, callback will do the rest
                ModelManager.runLater(() -> originalGs.getGroundStation().update(gs));
            }
        }
    }

    public void onNewOrbitAction(ActionEvent actionEvent) {
        Orbit gs = TleOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow());
        if(gs != null) {
            ModelManager.runLater(() -> manager.getOrbitManager().newOrbit(
                    gs.getCode(), gs.getName(), gs.getColor(), gs.isVisible(), gs.getModel()
            ));
        }
    }

    public void onNewCelestrakOrbitAction(ActionEvent actionEvent) {
        List<Orbit> theNewOrbits = CelestrakDialog.openDialog(scene3d.getParent().getScene().getWindow());
        if(theNewOrbits != null) {
            theNewOrbits.forEach(gs -> ModelManager.runLater(() -> manager.getOrbitManager().newOrbit(
                    gs.getCode(), gs.getName(), gs.getColor(), gs.isVisible(), gs.getModel()
            )));
        }
    }


    public void onNewOemOrbitAction(ActionEvent actionEvent) {
        Orbit gs = OemOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow());
        if(gs != null) {
            ModelManager.runLater(() -> manager.getOrbitManager().newOrbit(
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
                ModelManager.runLater(() -> {
                    String newTle = CelestrakTleData.retrieveUpdatedTle(theOrbit.getGroup(), orbit.getName());
                    if(newTle != null) {
                        CelestrakTleOrbitModel model = new CelestrakTleOrbitModel(theOrbit.getGroup(), newTle);
                        orbit.update(new Orbit(orbit.getId(), orbit.getCode(), orbit.getName(), orbit.getColor(), orbit.isVisible(), model));
                    }
                });
            }
        }
    }

    public void onDeleteOrbitAction(ActionEvent actionEvent) {
        OrbitGraphics orbit = orbitList.getSelectionModel().getSelectedItem();
        if(orbit != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Orbit");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to delete orbit for " + orbit.getName() + "?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK){
                ModelManager.runLater(() -> manager.getOrbitManager().removeOrbit(orbit.getOrbit().getId()));
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
                Orbit ob = CelestrakTleOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    ModelManager.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            } else if(orbit.getModel()  instanceof TleOrbitModel) {
                Orbit ob = TleOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    ModelManager.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            } else if(orbit.getModel()  instanceof OemOrbitModel) {
                Orbit ob = OemOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow(), orbit);
                if (ob != null) {
                    ModelManager.runLater(() -> originalOrbit.getOrbit().update(ob));
                }
            }
        }
    }

    public void onActivateTrackingAction(ActionEvent actionEvent) {
        if(this.timerTrackingButton.isSelected() && this.timerTask == null) {
            this.timerTask = new TimerTask() {
                @Override
                public void run() {
                    refreshModel(new Date(), false);
                }
            };
            this.tracker.schedule(timerTask, 0, 5000);
        } else if(!this.timerTrackingButton.isSelected() && this.timerTask != null){
            this.timerTask.cancel();
            this.timerTask = null;
        }
    }

    private void refreshModel(Date now, boolean forceUpdate) {
        ModelManager.runLater(() -> manager.getOrbitManager().updateOrbitTime(now, forceUpdate));
    }

    private OrbitGraphics getGroundTrackSelection() {
        Object selectedSc = groundTrackCombo.getSelectionModel().getSelectedItem();
        if(selectedSc.equals(NO_GROUND_TRACK)) {
            return null;
        } else {
            return (OrbitGraphics) selectedSc;
        }
    }

    public void onGroundTrackComboSelected(ActionEvent actionEvent) {
        update2Dscene();
    }

    private void updatePolarPlotSelection(VisibilityWindow c) {
        if(c == null) {
            this.polarPlotController.clear();
        } else {
            this.polarPlotController.setSpacecraftTrack(c);
            Color trackColor = Color.valueOf(c.getOrbit().getColor());
            this.polarPlotController.setTrackColor(trackColor);
            this.polarPlotController.setSpacecraftColor(trackColor);
            // Check if the spacecraft is in visibility
            SpacecraftPosition sp = c.getOrbit().getCurrentSpacecraftPosition();
            if(sp != null) {
                TrackPoint tp = c.getStation().getTrackPointOf(sp);
                if(tp != null && c.isInPass(tp.getTime())) {
                    this.polarPlotController.setNewSpacecraftPosition(c.getStation(), c.getOrbit(), tp);
                }
            }
        }
    }

    private void updateOrbitPanelSelection(OrbitGraphics c) {
        if(c == null) {
            this.orbitPanelController.clear();
        } else {
            this.orbitPanelController.update(c.getOrbit());
        }
    }

    @Override
    public void orbitAdded(OrbitManager manager, Orbit orbit) {
        Platform.runLater(() -> registerNewOrbit(orbit));
    }

    @Override
    public void orbitRemoved(OrbitManager manager, Orbit orbit) {
        Platform.runLater(() -> deregisterOrbit(orbit));
    }

    @Override
    public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        Platform.runLater(() -> {
            orbitList.refresh();
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void startOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        Platform.runLater(() -> {
            processingLabel.setBackground(new Background(new BackgroundFill(Color.valueOf("green"), null, null)));
            processingLabel.setText("UPDATE");
            orbitUpdateInProgress = true;
        });
    }

    @Override
    public void endOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        Platform.runLater(() -> {
            orbitUpdateInProgress = false;
            processingLabel.setBackground(null);
            processingLabel.setText("IDLE");
            update2Dscene();
        });
    }

    @Override
    public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        Platform.runLater(() -> {
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void groundStationAdded(GroundStationManager manager, GroundStation groundStation) {
        Platform.runLater(() -> registerNewGroundStation(groundStation));
    }

    @Override
    public void groundStationRemoved(GroundStationManager manager, GroundStation groundStation) {
        Platform.runLater(() -> deregisterGroundStation(groundStation));
    }

    @Override
    public void groundStationUpdated(GroundStation groundStation) {
        Platform.runLater(() -> {
            groundStationList.refresh();
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
        Platform.runLater(() -> {
            if(currentPoint != null) {
                this.currentTimeLabel.setText(TimeUtils.formatDate(currentPoint.getTime()));
            }
            this.polarPlotController.updateCurrentData(groundStation, orbit, visibilityWindows);
            this.polarPlotController.setNewSpacecraftPosition(groundStation, orbit, currentPoint);
            if(groundStationList.getSelectionModel().getSelectedItem() != null && groundStation.equals(groundStationList.getSelectionModel().getSelectedItem().getGroundStation())) {
                // Force refresh of visibility windows
                updatePassTableSelection(groundStationList.getSelectionModel().getSelectedItem());
            }
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    @Override
    public void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        Platform.runLater(() -> {
            if(point != null) {
                this.currentTimeLabel.setText(TimeUtils.formatDate(point.getTime()));
            }
            this.polarPlotController.setNewSpacecraftPosition(groundStation, orbit, point);
            if(!orbitUpdateInProgress) {
                update2Dscene();
            }
        });
    }

    public void onExportOemOrbitAction(ActionEvent actionEvent) {
        OrbitGraphics originalOrbit = orbitList.getSelectionModel().getSelectedItem();
        if(originalOrbit != null) {
            Orbit orbit = originalOrbit.getOrbit();
            ExportOemOrbitDialog.ExportOemResult exportOemResult = ExportOemOrbitDialog.openDialog(scene3d.getParent().getScene().getWindow(), orbit);
            if(exportOemResult != null) {
                ModelManager.runLater(() -> {
                    try {
                        manager.getOrbitManager().exportOem(
                                orbit.getId(),
                                exportOemResult.getCode(),
                                exportOemResult.getName(),
                                exportOemResult.getStartTime(),
                                exportOemResult.getEndTime(),
                                exportOemResult.getPeriodSeconds(),
                                exportOemResult.getFile(),
                                exportOemResult.getFrame(),
                                exportOemResult.getFormat());
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("OEM Export");
                            alert.setHeaderText("Orbit of " + orbit.getName() + " exported");
                            alert.setContentText("OEM file: " + exportOemResult.getFile());
                            alert.showAndWait();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("OEM Export");
                            alert.setHeaderText("Orbit of " + orbit.getName() + " not exported");
                            alert.setContentText("Error: " + e.getMessage());
                            alert.showAndWait();
                        });
                    }
                });
            }
        }
    }

    public void onForceOrbitComputationAction(ActionEvent actionEvent) {
        refreshModel(new Date(), true);
    }

    public void onGenerateScheduleAction(ActionEvent actionEvent) {
        GroundStationGraphics gs = groundStationList.getSelectionModel().getSelectedItem();
        if(gs != null) {
            List<Orbit> orbits = orbitList.getItems().stream().map(OrbitGraphics::getOrbit).collect(Collectors.toList());
            // open dialog
            ScheduleGenerationRequest sgr = ExportScheduleDialog.openDialog(groundStationList.getScene().getWindow(), gs.getGroundStation(), orbits);
            if(sgr != null) {
                ModelManager.runLater(() -> {
                    try {
                        manager.exportSchedule(sgr);
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("CCSDS Simple Schedule Export");
                            alert.setHeaderText("Schedule of " + gs.getGroundStation().getName() + " exported");
                            alert.setContentText("Schedule file: " + sgr.getFilePath());
                            alert.showAndWait();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("CCSDS Simple Schedule Export");
                            alert.setHeaderText("Schedule of " + gs.getGroundStation().getName() + " not exported");
                            alert.setContentText("Error: " + e.getMessage());
                            alert.showAndWait();
                        });
                    }
                });
            }
        }
    }
}
