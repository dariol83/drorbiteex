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

package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class OrbitVisibilityAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static String analyse(OrbitVisibilityAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        Orbit refOrbit = request.getReferenceOrbit();
        List<GroundStation> targetGroundStations = request.getTargetGroundStations();

        // Set configuration to all orbits
        OrbitParameterConfiguration orbitConf = refOrbit.getOrbitConfiguration().copy();
        // Never recompute orbit and do not propagate every time
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setAfterPropagationSteps(0);
        orbitConf.setBeforePropagationSteps(0);
        refOrbit.setOrbitConfiguration(orbitConf);

        ExecutorService service = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "Orbit Visibility Analyser Task");
            t.setDaemon(true);
            return t;
        });
        Worker chunkWorker = new Worker(refOrbit, targetGroundStations, request, monitor);
        Future<List<VisibilityWindow>> futureTask = service.submit(chunkWorker);
        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        try {
            if (monitor.isCancelled()) {
                service.shutdownNow();
                return null;
            }
            List<VisibilityWindow> windows = futureTask.get();
            if(windows == null) {
                return null;
            }
            // Export file
            String file = exportVisibilityWindows(request.getStartTime(), request.getEndTime(), request.getExportFolder(), request.getReferenceOrbit().getCode(), windows);
            // Done
            monitor.progress(1, 1, "Done");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            System.gc();
        }
    }

    private static String exportVisibilityWindows(Date start, Date end, String exportFolder, String orbitCode, List<VisibilityWindow> windows) throws FileNotFoundException {
        String fileName = orbitCode + "_VW_G" +
                TimeUtils.formatDate(new Date()).replace(" ","").replace(".","").replace(":","")
                + "_S" +
                TimeUtils.formatDate(start).replace(" ","").replace(".","").replace(":","")
                + "_E" +
                TimeUtils.formatDate(end).replace(" ","").replace(".","").replace(":","")
                + ".csv";
        File exportFile = new File(exportFolder + File.separator + fileName);
        PrintStream ps = new PrintStream(new FileOutputStream(exportFile));
        for(VisibilityWindow visibilityWindow : windows) {
            ps.printf("%s,%s,%d,%s,%s%n", visibilityWindow.getOrbit().getCode(), visibilityWindow.getStation().getCode(), visibilityWindow.getOrbitNumber(),
                    TimeUtils.formatDate(visibilityWindow.getAos()), TimeUtils.formatDate(visibilityWindow.getLos()));
        }
        ps.close();
        return exportFile.getAbsolutePath();
    }

    private static class Worker implements Callable<List<VisibilityWindow>> {
        private final Orbit referenceOrbit;
        private final Date startTime;
        private final Date endTime;
        private final List<GroundStation> targetGroundStations = new LinkedList<>();
        private final ITaskProgressMonitor monitor;

        public Worker(Orbit referenceOrbit, List<GroundStation> targetGroundStations,
                      OrbitVisibilityAnalysisRequest request, ITaskProgressMonitor monitor) {
            this.referenceOrbit = referenceOrbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            for(GroundStation gs : targetGroundStations) {
                GroundStation cloned = gs.copy();
                this.targetGroundStations.add(cloned);
            }
            this.startTime = request.getStartTime();
            this.endTime = request.getEndTime();
            this.monitor = monitor;
        }

        @Override
        public String toString() {
            return TimeUtils.formatDate(startTime) + " - " + TimeUtils.formatDate(endTime) + ": visibility of " + referenceOrbit;
        }

        @Override
        public List<VisibilityWindow> call() {
            monitor.progress(0, 10, "Initialising");
            if(monitor.isCancelled()) {
                return null;
            }
            // Perform the propagation
            AbsoluteDate startDate = TimeUtils.toAbsoluteDate(startTime);
            AbsoluteDate endDate = TimeUtils.toAbsoluteDate(endTime);
            referenceOrbit.getModel().getPropagator().propagate(startDate);
            // Future, register event detectors from listeners
            for(GroundStation clonedStation : targetGroundStations) {
                EventDetector detector = clonedStation.getEventDetector();
                referenceOrbit.getModel().getPropagator().addEventDetector(detector);
                clonedStation.initVisibilityComputation(referenceOrbit, startDate.toDate(TimeScalesFactory.getUTC()));
            }
            if(monitor.isCancelled()) {
                return null;
            }
            monitor.progress(2, 10, "Propagating");
            // Propagate to end date
            referenceOrbit.getModel().getPropagator().propagate(endDate);
            if(monitor.isCancelled()) {
                return null;
            }
            monitor.progress(8, 10, "Cleaning up");
            // Declare end for detectors, clear detectors
            for(GroundStation clonedStation : targetGroundStations) {
                clonedStation.finalizeVisibilityComputation(referenceOrbit, null);
            }
            if(monitor.isCancelled()) {
                return null;
            }
            referenceOrbit.getModel().getPropagator().clearEventsDetectors();
            // Now: for every listener, move back the model propagation to the current date and offer the propagator to
            // each listener for visibility use (GroundStation) or other use.
            for(GroundStation clonedStation : targetGroundStations) {
                clonedStation.endVisibilityComputation(referenceOrbit);
            }
            if(monitor.isCancelled()) {
                return null;
            }
            // Reset the propagator after every use
            referenceOrbit.getModel().getPropagator().propagate(startDate);
            for(GroundStation clonedStation : targetGroundStations) {
                clonedStation.propagationModelAvailable(referenceOrbit, startTime, referenceOrbit.getModel().getPropagator());
            }
            // Return the passes that are completed - passes with null AOS or null LOS (open passes) must be discarded
            List<VisibilityWindow> toReturn = new LinkedList<>();
            for(GroundStation clonedStation : targetGroundStations) {
                toReturn.addAll(
                    clonedStation.getVisibilityWindowsOf(referenceOrbit).stream().filter(o -> o.getAos() != null && o.getLos() != null).collect(Collectors.toList())
                );
            }
            // Sort visibilities
            toReturn.sort(this::visibilityWindowCompare);
            if(monitor.isCancelled()) {
                return null;
            }
            monitor.progress(9, 10, "Exporting");
            return toReturn;
        }

        private int visibilityWindowCompare(VisibilityWindow v1, VisibilityWindow v2) {
            int result = v1.getAos().compareTo(v2.getAos());
            if(result == 0) {
                // Order by station code
                return v1.getStation().getCode().compareTo(v2.getStation().getCode());
            } else {
                return result;
            }
        }
    }
}
