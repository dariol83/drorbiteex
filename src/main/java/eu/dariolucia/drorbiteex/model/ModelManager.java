package eu.dariolucia.drorbiteex.model;

import eu.dariolucia.drorbiteex.model.orbit.IOrbitListener;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitManager;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.station.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModelManager implements IOrbitListener, IGroundStationListener {

    private static final Logger LOGGER = Logger.getLogger(ModelManager.class.getName());

    private final OrbitManager orbitManager;
    private final GroundStationManager groundStationManager;

    private final String orbitFileStorage;
    private final String groundStationFileStorage;

    private static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor((e) -> {
        Thread t = new Thread(e);
        t.setDaemon(true);
        t.setName("Dr Orbiteex - Model Manager Thread");
        return t;
    });

    public ModelManager(String orbitFileStorage, String groundStationFileStorage) {
        this.orbitFileStorage = orbitFileStorage;
        this.groundStationFileStorage = groundStationFileStorage;
        this.orbitManager = new OrbitManager();
        File orbitFile = new File(orbitFileStorage);
        if(orbitFile.exists()) {
            try {
                this.orbitManager.initialise(new FileInputStream(orbitFileStorage));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot initialise orbit manager", e);
            }
        }
        this.groundStationManager = new GroundStationManager();
        File groundStationFile = new File(groundStationFileStorage);
        if(groundStationFile.exists()) {
            try {
                this.groundStationManager.initialise(new FileInputStream(groundStationFileStorage));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot initialise ground station manager", e);
            }
        }
        // Add all ground stations as listeners to the orbits
        for(GroundStation gs : this.groundStationManager.getGroundStations().values()) {
            this.orbitManager.addListener(gs);
        }
        // Register the manager as listener
        this.orbitManager.addListener(this);
        this.groundStationManager.addListener(this);
    }

    public static void runLater(Runnable e) {
        THREAD_EXECUTOR.submit(e);
    }

    public OrbitManager getOrbitManager() {
        return orbitManager;
    }

    public GroundStationManager getGroundStationManager() {
        return groundStationManager;
    }

    @Override
    public void orbitAdded(OrbitManager manager, Orbit orbit) {
        LOGGER.log(Level.INFO, "New orbit added: " + orbit);
        // If an orbit is added, recompute all orbital parameters
        this.orbitManager.refresh();
        // Save configuration
        saveOrbitFile();
    }

    private void saveOrbitFile() {
        try {
            File orbitFile = new File(orbitFileStorage);
            if(!orbitFile.exists()) {
                orbitFile.getParentFile().mkdirs();
                orbitFile.createNewFile();
            }
            this.orbitManager.persist(new FileOutputStream(orbitFile));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot save orbit manager", e);
        }
    }

    @Override
    public void orbitRemoved(OrbitManager manager, Orbit orbit) {
        LOGGER.log(Level.INFO, "Orbit removed: " + orbit);
        // Save configuration
        saveOrbitFile();
    }

    @Override
    public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        // Save configuration
        saveOrbitFile();
    }

    @Override
    public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        // Nothing to do
    }

    @Override
    public void groundStationAdded(GroundStationManager manager, GroundStation groundStation) {
        LOGGER.log(Level.INFO, "New ground station added: " + groundStation);
        // Register the ground station to the orbits
        this.orbitManager.addListener(groundStation);
        // Save configuration
        saveGroundStationFile();
        // Request a orbit refresh
        this.orbitManager.refresh();
    }

    private void saveGroundStationFile() {
        try {
            File stationFile = new File(groundStationFileStorage);
            if(!stationFile.exists()) {
                stationFile.getParentFile().mkdirs();
                stationFile.createNewFile();
            }
            this.groundStationManager.persist(new FileOutputStream(stationFile));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot save ground station manager", e);
        }
    }

    @Override
    public void groundStationRemoved(GroundStationManager manager, GroundStation groundStation) {
        LOGGER.log(Level.INFO, "Ground station removed: " + groundStation);
        // Deregister the ground station to the orbits
        this.orbitManager.removeListener(groundStation);
        // Save configuration
        saveGroundStationFile();
    }

    @Override
    public void groundStationUpdated(GroundStation groundStation) {
        LOGGER.log(Level.INFO, "Ground station updated: " + groundStation);
        // Save configuration
        saveGroundStationFile();
        // Position of the GS could have changed: request a orbit refresh
        this.orbitManager.refresh();
    }

    @Override
    public void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
        // Nothing to do
    }

    @Override
    public void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point) {
        // Nothing to do
    }
}
