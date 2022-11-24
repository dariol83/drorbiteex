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

package eu.dariolucia.drorbiteex.model;

import eu.dariolucia.drorbiteex.model.orbit.*;
import eu.dariolucia.drorbiteex.model.schedule.CcsdsSimpleScheduleExporter;
import eu.dariolucia.drorbiteex.model.schedule.IScheduleExporter;
import eu.dariolucia.drorbiteex.model.schedule.ScheduleExporterRegistry;
import eu.dariolucia.drorbiteex.model.schedule.ScheduleGenerationRequest;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    public void updateOrbitParameters(OrbitParameterConfiguration configuration) {
        this.orbitManager.updateConfiguration(configuration);
        saveOrbitFile();
    }

    public void updateGroundStationParameters(GroundStationParameterConfiguration props) {
        this.groundStationManager.updateConfiguration(props);
        saveGroundStationFile();
        // Refresh
        orbitManager.refresh();
    }

    public OrbitManager getOrbitManager() {
        return orbitManager;
    }

    public GroundStationManager getGroundStationManager() {
        return groundStationManager;
    }

    public void exportSchedule(ScheduleGenerationRequest request) throws IOException {
        // For all the provided orbits, get a copy of the model and compute the visibility windows between the dates
        IScheduleExporter externalExporter = ScheduleExporterRegistry.instance().getExporter(request.getExporterToUse());
        CcsdsSimpleScheduleExporter exporter = new CcsdsSimpleScheduleExporter(request.getFilePath(), externalExporter);
        exporter.writeHeader(request);
        // Go for passes
        for(Orbit o : request.getOrbits()) {
            List<VisibilityWindow> passes = computePasses(request.getGroundStation(), o, request.getStartTime(), request.getEndTime());
            exporter.writeScheduledPackage(request, request.getGroundStation(), o, passes);
        }
        exporter.close();
    }

    // TODO: refactor to avoid duplication with method in Orbit class
    private List<VisibilityWindow> computePasses(GroundStation groundStation, Orbit orbit, Date startTime, Date endTime) {
        // Clone the orbit and the ground station
        Orbit clonedOrbit = new Orbit(orbit.getId(), orbit.getCode(), orbit.getName(), orbit.getColor(), orbit.isVisible(), orbit.getModel().copy());
        clonedOrbit.setOrbitConfiguration(orbitManager.getConfiguration());
        GroundStation clonedStation = new GroundStation(groundStation.getId(), groundStation.getCode(), groundStation.getName(), groundStation.getSite(), groundStation.getDescription(), groundStation.getColor(),
                groundStation.isVisible(), groundStation.getLatitude(), groundStation.getLongitude(), groundStation.getHeight());
        clonedStation.setConfiguration(groundStation.getConfiguration());
        // Perform the propagation
        AbsoluteDate startDate = TimeUtils.toAbsoluteDate(startTime);
        AbsoluteDate endDate = TimeUtils.toAbsoluteDate(endTime);
        clonedOrbit.getModel().getPropagator().propagate(startDate);
        // Future, register event detectors from listeners
        EventDetector detector = clonedStation.getEventDetector();
        clonedOrbit.getModel().getPropagator().addEventDetector(detector);
        clonedStation.initVisibilityComputation(clonedOrbit, startDate.toDate(TimeScalesFactory.getUTC()));
        // Propagate to end date
        clonedOrbit.getModel().getPropagator().propagate(endDate);
        // Declare end for detectors, clear detectors
        clonedStation.finalizeVisibilityComputation(clonedOrbit, null);
        clonedOrbit.getModel().getPropagator().clearEventsDetectors();
        // Now: for every listener, move back the model propagation to the current date and offer the propagator to
        // each listener for visibility use (GroundStation) or other use.
        clonedStation.endVisibilityComputation(clonedOrbit);

        // Reset the propagator after every use
        clonedOrbit.getModel().getPropagator().propagate(startDate);
        clonedStation.propagationModelAvailable(clonedOrbit, startTime, clonedOrbit.getModel().getPropagator());
        // Return the passes that are completed - passes with null AOS or null LOS (open passes) must be discarded
        return clonedStation.getVisibilityWindowsOf(clonedOrbit).stream().filter(o -> o.getAos() != null && o.getLos() != null).collect(Collectors.toList());
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
