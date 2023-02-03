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

import eu.dariolucia.drorbiteex.fxml.PolarPlotPainter;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class SkyCoverageAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };
    private static final long DAY_MS = 3600L * 24000L;

    public static Canvas analyse(SkyCoverageAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        if(request.getCelestrakGroup() == null && request.getTargetOrbits() == null) {
            throw new IllegalArgumentException("One between Celestrak group and target orbits must be specified");
        }
        List<CollinearityEvent> events;
        // Create the orbits
        List<Orbit> targetOrbits;
        if(request.getCelestrakGroup() != null) {
            monitor.progress(-1, -1, "Fetching Celestrak data for group " + request.getCelestrakGroup());
            // Fetch all selected satellites in Celestrak
            List<CelestrakTleData> active = CelestrakTleData.retrieveSpacecraftList(request.getCelestrakGroup());
            if(active == null) {
                throw new IOException("Cannot fetch Celestrak data for '" + request.getCelestrakGroup() + "'satellites");
            }
            if(monitor.isCancelled()) {
                return null;
            }
            targetOrbits = active
                    .stream()
                    .map(o -> new Orbit(UUID.randomUUID(), o.getName(), o.getName(), "#FF0000", true, new CelestrakTleOrbitModel(o.getGroup(), o.getName(), o.getTle())))
                    .collect(Collectors.toList());
        } else {
            targetOrbits = request.getTargetOrbits().stream().map(Orbit::copy).peek(o -> o.setColor("#FF0000")).collect(Collectors.toList());
        }
        // Filter out orbits
        monitor.progress(-1, -1, "Filtering " + targetOrbits.size() + " orbits...");
        targetOrbits = targetOrbits
                .stream()
                // remove the orbits that match the exclusions
                .filter(o -> {
                    for (String s : request.getOrbitExclusions()) {
                        if (o.getName().toLowerCase().contains(s.toLowerCase())) {
                            // Filter out
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        // Set configuration to all orbits
        monitor.progress(-1, -1, "Configuring " + targetOrbits.size() + " initial orbits...");
        OrbitParameterConfiguration orbitConf = new OrbitParameterConfiguration();
        // Never recompute orbit and do single propagation for the entire duration of the period (compute it here)
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setBeforePropagationSteps(0);
        int stepInterval = 180;
        orbitConf.setStepInterval(stepInterval); // 3 minutes
        // Propagation step computation
        int numOfSteps = (int) (((request.getEndTime().getTime() - request.getStartTime().getTime())/1000)/stepInterval);
        orbitConf.setAfterPropagationSteps(numOfSteps);
        for(Orbit o : targetOrbits) {
            o.setOrbitConfiguration(orbitConf);
        }

        // One satellite, one thread: use thread pool
        int threadsToUse = request.getCores();
        ExecutorService service = Executors.newFixedThreadPool(threadsToUse, (r) -> {
            Thread t = new Thread(r, "Sky Coverage Analyser Task");
            t.setDaemon(true);
            return t;
        });

        monitor.progress(-1, -1, "Scheduling " + targetOrbits.size() + " orbit propagations...");
        List<WorkerFutureTask> futures = new LinkedList<>();
        for (Orbit tOrbit : targetOrbits) {
            if (monitor.isCancelled()) {
                service.shutdownNow();
                return null;
            }
            Worker chunkWorker = new Worker(request, tOrbit);
            WorkerFutureTask futureTask = new WorkerFutureTask(chunkWorker);
            service.submit(futureTask);
            futures.add(futureTask);
        }

        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        Canvas output = new Canvas(400, 400);
        GraphicsContext gc = output.getGraphicsContext2D();
        PolarPlotPainter painter = new PolarPlotPainter(gc, output.getWidth(), output.getHeight());
        painter.drawBackground(Color.BLACK);
        painter.drawPlot(Color.LIMEGREEN);
        monitor.progress(-1, -1, "Processing...");
        long progress = 0;
        for(WorkerFutureTask f : futures) {
            try {
                List<VisibilityWindow> subEventList = f.get();
                for(VisibilityWindow vw : subEventList) {
                    if(monitor.isCancelled()) {
                        service.shutdownNow();
                        return null;
                    }
                    printTrack(vw, painter);
                }
                // Clean up visibilities for orbit
                if(subEventList.size() > 0) {
                    subEventList.get(0).getStation().clearVisibilityWindowsOf(subEventList.get(0).getOrbit());
                }
                //
                if(monitor.isCancelled()) {
                    service.shutdownNow();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                service.shutdownNow();
                throw new IOException(e);
            } finally {
                // Free up resources
                System.gc();
            }
            ++progress;
            monitor.progress(progress, futures.size(), f.getDescription());
        }
        // Super-impose the plot again
        painter.drawPlot(Color.LIMEGREEN);
        // Return the events
        return output;
    }

    private static void printTrack(VisibilityWindow vw, PolarPlotPainter painter) {
        painter.drawTrack(Color.WHITE, vw.getGroundTrack());
    }

    private static class Worker implements Callable<List<VisibilityWindow>> {
        private final Orbit referenceOrbit;
        private final SkyCoverageAnalysisRequest request;
        private final GroundStation groundStation;

        public Worker(SkyCoverageAnalysisRequest request, Orbit orbit) {
            this.groundStation = request.getGroundStation().copy();
            GroundStationParameterConfiguration gsc = groundStation.getConfiguration();
            gsc.setTrackingInterval(30);
            this.groundStation.setConfiguration(gsc);
            this.referenceOrbit = orbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.request = request;
        }

        @Override
        public String toString() {
            return referenceOrbit.toString();
        }

        @Override
        public List<VisibilityWindow> call() {
            // Register the ground station to the orbit
            this.referenceOrbit.addListener(this.groundStation);
            // Propagate the reference orbit
            this.referenceOrbit.updateOrbitTime(this.request.getStartTime(), true);
            // Get the visibility windows
            return this.groundStation.getVisibilityWindowsOf(this.referenceOrbit);
        }
    }

    private static class WorkerFutureTask extends FutureTask<List<VisibilityWindow>> {

        private final String description;

        public WorkerFutureTask(Worker callable) {
            super(callable);
            this.description = callable.toString();
        }

        public String getDescription() {
            return description;
        }
    }
}
