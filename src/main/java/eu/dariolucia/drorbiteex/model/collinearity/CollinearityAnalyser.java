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

import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
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

public class CollinearityAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };
    private static final long DAY_MS = 3600L * 24000L;

    public static List<CollinearityEvent> analyse(CollinearityAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
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
        GroundStation groundStation = request.getGroundStation();
        Orbit refOrbit = request.getReferenceOrbit();
        // Create the orbits
        String referenceOrbitCelestrakName = refOrbit.getModel() instanceof CelestrakTleOrbitModel ? ((CelestrakTleOrbitModel) refOrbit.getModel()).getCelestrakName() : null;
        List<Orbit> targetOrbits;
        if(request.getCelestrakGroup() != null) {
            monitor.progress(-1, -1, "Fetching Celestrak data for group " + request.getCelestrakGroup());
            // Fetch all selected satellites in Celestrak
            List<CelestrakTleData> active = CelestrakTleData.retrieveSpacecraftList(request.getCelestrakGroup());
            if(active == null) {
                throw new IOException("Cannot fetch Celestrak data for '" + request.getCelestrakGroup() + "' satellites");
            }
            if(monitor.isCancelled()) {
                return null;
            }
            targetOrbits = active
                    .stream()
                    .map(o -> new Orbit(UUID.randomUUID(), o.getName(), o.getName(), "#FF0000", true, new CelestrakTleOrbitModel(o.getGroup(), o.getName(), o.getTle())))
                    .collect(Collectors.toList());
        } else {
            targetOrbits = request.getTargetOrbits().stream().map(Orbit::copy).collect(Collectors.toList());
        }
        // Filter out orbits
        monitor.progress(-1, -1, "Filtering " + targetOrbits.size() + " orbits...");
        targetOrbits = targetOrbits
                .stream()
                // remove the orbit if Celestrak based or same name or same UUID
                .filter(o -> {
                    if ((referenceOrbitCelestrakName != null && ((CelestrakTleOrbitModel) o.getModel()).getCelestrakName().equals(referenceOrbitCelestrakName)) ||
                            o.getId().equals(refOrbit.getId())) {
                        return false;
                    } else {
                        return !o.getName().equals(request.getReferenceOrbit().getName());
                    }
                })
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
        OrbitParameterConfiguration orbitConf = refOrbit.getOrbitConfiguration().copy();
        // Never recompute orbit and do not propagate every time
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setAfterPropagationSteps(0);
        orbitConf.setBeforePropagationSteps(0);
        for(Orbit o : targetOrbits) {
            o.setOrbitConfiguration(orbitConf);
        }

        // One chunk (time span and satellite), one thread: use thread pool
        int threadsToUse = request.getCores();
        ExecutorService service = Executors.newFixedThreadPool(threadsToUse, (r) -> {
            Thread t = new Thread(r, "Collinearity Analyser Task");
            t.setDaemon(true);
            return t;
        });

        List<WorkerFutureTask> futures = new LinkedList<>();
        // Start time (day) and satellite based division
        Date currentTime = request.getStartTime();
        Date endTime = request.getEndTime();
        while(currentTime.getTime() <= endTime.getTime()) {
            Date currentEndTime = (currentTime.getTime() + DAY_MS < endTime.getTime()) ? new Date(currentTime.getTime() + DAY_MS) : endTime;
            for (Orbit tOrbit : targetOrbits) {
                if (monitor.isCancelled()) {
                    service.shutdownNow();
                    return null;
                }
                Worker chunkWorker = new Worker(groundStation, refOrbit, currentTime, currentEndTime, Collections.singletonList(tOrbit), request.getIntervalPeriod(), request);
                WorkerFutureTask futureTask = new WorkerFutureTask(chunkWorker);
                service.submit(futureTask);
                futures.add(futureTask);
            }
            currentTime = new Date(currentEndTime.getTime() + 1);
        }

        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        events = new LinkedList<>();
        long progress = 0;
        for(WorkerFutureTask f : futures) {
            try {
                List<CollinearityEvent> subEventList = f.get();
                if(monitor.isCancelled()) {
                    service.shutdownNow();
                    return null;
                }
                events.addAll(subEventList);
            } catch (Exception e) {
                e.printStackTrace();
                service.shutdownNow();
                throw new IOException(e);
            } finally {
                System.gc();
            }
            ++progress;
            monitor.progress(progress, futures.size(), f.getDescription());
        }

        // Return the events
        return events;
    }

    public static void generateCSV(String filePath, List<CollinearityEvent> events) throws IOException {
        File toGenerate = new File(filePath);
        if(toGenerate.exists()) {
            toGenerate.delete();
        }
        toGenerate.createNewFile();
        PrintStream ps = new PrintStream(new FileOutputStream(toGenerate));
        // Write header
        ps.println(CollinearityEvent.getCsvHeader());
        // Write events
        events.forEach(e -> ps.println(e.toCSV()));
        // Close
        ps.close();
    }

    private static class Worker implements Callable<List<CollinearityEvent>> {
        private final GroundStation groundStation;
        private final Orbit referenceOrbit;
        private final Date start;
        private final Date end;
        private final List<Orbit> targetOrbits;
        private final int pointInterval;
        private final CollinearityAnalysisRequest request;

        public Worker(GroundStation groundStation, Orbit referenceOrbit, Date start, Date end,
                      List<Orbit> targetOrbits, int pointInterval,
                      CollinearityAnalysisRequest request) {
            this.groundStation = groundStation.copy();
            this.groundStation.setReducedProcessing();
            this.groundStation.setConfiguration(groundStation.getConfiguration());
            this.referenceOrbit = referenceOrbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.start = start;
            this.end = end;
            this.targetOrbits = targetOrbits.stream().map(Orbit::copy).collect(Collectors.toList());
            this.targetOrbits.forEach(o -> o.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration()));
            this.pointInterval = pointInterval * 1000;
            this.request = request;
        }

        @Override
        public String toString() {
            return TimeUtils.formatDate(start) + " - " + TimeUtils.formatDate(end) + ": " + targetOrbits.get(0);
        }

        @Override
        public List<CollinearityEvent> call() {
            // Register the ground station to the orbits
            this.targetOrbits.forEach(o -> o.addListener(this.groundStation));
            this.referenceOrbit.addListener(this.groundStation);
            // Instantiate the data collector
            CollinearityDataCollector dataCollector = new CollinearityDataCollector(this.request);
            this.groundStation.addListener(dataCollector);
            // Iterate from start to end
            Date currentDate = start;
            while(currentDate.before(end)) {
                // Propagate the reference orbit
                referenceOrbit.updateOrbitTime(currentDate, false);
                // Propagate the target orbits
                for(Orbit o : targetOrbits) {
                    o.updateOrbitTime(currentDate, false);
                }
                // Next step
                currentDate = new Date(currentDate.getTime() + this.pointInterval);
            }
            // Get the events
            return dataCollector.getEvents();
        }
    }

    private static class WorkerFutureTask extends FutureTask<List<CollinearityEvent>> {

        private final String description;

        public WorkerFutureTask(Worker callable) {
            super(callable);
            this.description = callable.toString();
        }

        public String getDescription() {
            return description;
        }
    }

    private static class CollinearityDataCollector implements IGroundStationListener {
        private final CollinearityAnalysisRequest request;
        private final List<CollinearityEvent> events = new LinkedList<>();
        private final Orbit referenceOrbit;
        private final double minAngularSeparation; // in degrees
        private Date currentDate = null;
        private TrackPoint referenceOrbitCurrentPosition = null;
        private Vector3D groundStationPoint;
        private Vector3D currentReferenceVector = null;

        public CollinearityDataCollector(CollinearityAnalysisRequest request) {
            this.referenceOrbit = request.getReferenceOrbit();
            this.minAngularSeparation = request.getMinAngularSeparation();
            this.request = request;
        }

        public List<CollinearityEvent> getEvents() {
            return events;
        }

        @Override
        public void groundStationAdded(GroundStationManager manager, GroundStation groundStation) {
            // Nothing
        }

        @Override
        public void groundStationRemoved(GroundStationManager manager, GroundStation groundStation) {
            // Nothing
        }

        @Override
        public void groundStationUpdated(GroundStation groundStation) {
            // Nothing
        }

        @Override
        public void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
            // Nothing
        }

        @Override
        public void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point) {
            // Ground station point in earth coordinates
            if(this.groundStationPoint == null) {
                this.groundStationPoint = EarthReferenceUtils.getReferenceEllipsoid().transform(groundStation.getStationFrame().getPoint());
            }
            // Compute s/c position data
            if(orbit.equals(this.referenceOrbit)) {
                // Elevation of target orbit < 0 --> return
                if(point == null || point.getElevation() < 0) {
                    return;
                }
                // Reference s/c: save info ...
                this.currentDate = point.getTime();
                this.referenceOrbitCurrentPosition = point;
                // ... and compute the reference (normalized) vector
                this.currentReferenceVector = computeVector(point, this.groundStationPoint);
            } else {
                // Target s/c: check collinearity if time is the same (with tolerance)
                // No reference orbit info --> return
                if(this.currentDate == null || this.referenceOrbitCurrentPosition == null) {
                    return;
                }
                // Elevation of reference orbit < 0 --> return
                if(this.referenceOrbitCurrentPosition.getElevation() < 0) {
                    return;
                }
                // Elevation of target orbit < 0 --> return
                if(point == null || point.getElevation() < 0) {
                    return;
                }
                // Height of target orbit > max height or < min height --> return
                if(point.getSpacecraftPosition().getLatLonHeight().getAltitude()/1000 > request.getMaxHeight()
                || point.getSpacecraftPosition().getLatLonHeight().getAltitude()/1000 < request.getMinHeight()) {
                    return;
                }
                // Time discrepancy --> return
                if(Math.abs(this.currentDate.getTime() - point.getTime().getTime()) > 1000) {
                    return;
                }
                // If we are here, it means that both satellites are in visibility --> Compute angular separation:
                // acos(dot product of normalised position vectors wrt ground station)
                Vector3D targetVector = computeVector(point, this.groundStationPoint);
                double result = this.currentReferenceVector.dotProduct(targetVector);
                double angularSeparation = Math.abs(Math.acos(result));
                angularSeparation = Math.toDegrees(angularSeparation);
                if(angularSeparation <= this.minAngularSeparation) {
                    CollinearityEvent event = new CollinearityEvent(groundStation, this.referenceOrbit, orbit, point.getTime().toInstant(), this.referenceOrbitCurrentPosition, point, angularSeparation);
                    // Create collinearity event
                    this.events.add(event);
                }
            }
        }

        public static Vector3D computeVector(TrackPoint point, Vector3D groundStationPoint) {
            return point.getSpacecraftPosition().computeVisibilityVectorFrom(groundStationPoint);
        }
    }
}
