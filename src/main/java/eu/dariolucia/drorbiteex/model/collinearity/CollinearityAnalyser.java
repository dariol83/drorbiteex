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
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CollinearityAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };
    private static final long DAY_MS = 3600 * 24000;

    public static List<CollinearityEvent> analyse(CollinearityAnalysisRequest request) throws IOException {
        return analyse(request, DUMMY_MONITOR);
    }
    public static List<CollinearityEvent> analyse(CollinearityAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            throw new NullPointerException("Monitor implementation cannot be null");
        }
        if(monitor.isCancelled()) {
            return null;
        }
        List<CollinearityEvent> events;
        GroundStation groundStation = request.getGroundStation();
        Orbit refOrbit = request.getReferenceOrbit();
        // Fetch all 'active' satellites in Celestrak
        List<CelestrakTleData> active = CelestrakTleData.retrieveSpacecraftList("active");
        if(active == null) {
            throw new IOException("Cannot fetch Celestrak data for 'active' satellites");
        }
        if(monitor.isCancelled()) {
            return null;
        }
        // Create the orbits, remove the reference orbit if Celestrak based or same name
        String referenceOrbitCelestrakName = refOrbit.getModel() instanceof CelestrakTleOrbitModel ? ((CelestrakTleOrbitModel) refOrbit.getModel()).getCelestrakName() : null;
        List<Orbit> targetOrbits = active
                .stream()
                .map(o -> new Orbit(UUID.randomUUID(), o.getName(), o.getName(), "#FFFFFF", true, new CelestrakTleOrbitModel(o.getGroup(), o.getName(), o.getTle())))
                .filter(o -> {
            if(referenceOrbitCelestrakName != null && ((CelestrakTleOrbitModel) o.getModel()).getCelestrakName().equals(referenceOrbitCelestrakName)) {
                return false;
            } else {
                return !o.getName().equals(request.getReferenceOrbit().getName());
            }
        }).collect(Collectors.toList());
        // Set configuration to all orbits
        OrbitParameterConfiguration orbitConf = refOrbit.getOrbitConfiguration().copy();
        // Never recompute orbit and do not propagate every time
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setAfterPropagationSteps(0);
        orbitConf.setBeforePropagationSteps(0);
        for(Orbit o : targetOrbits) {
            o.setOrbitConfiguration(orbitConf);
        }

        // One chunk, one thread: use thread pool
        int threadsToUse = request.getCores();
        System.out.println("Using " + threadsToUse + " threads");
        ExecutorService service = Executors.newFixedThreadPool(threadsToUse, (r) -> {
            Thread t = new Thread(r, "Collinearity Job");
            t.setDaemon(true);
            return t;
        });

        List<Future<List<CollinearityEvent>>> futures = new LinkedList<>();
        // Start chunk based division
        // Calculate the chunks in terms of times, and create the jobs to execute
        /*
        long chunkDuration = ORBIT_POINT_INTERVAL * ORBIT_COMPUTATION_CHUNK; // in milliseconds
        long startChunkTime = request.getStartTime().getTime();
        long endComputationTime = request.getEndTime().getTime();
        while(startChunkTime < endComputationTime) {
            // Compute the end chunk time
            long endChunkTime = startChunkTime + chunkDuration;
            if(endChunkTime < endComputationTime) {
                // This is a chunk
                futures.add(service.submit(new Worker(groundStation, refOrbit, new Date(startChunkTime), new Date(endChunkTime), targetOrbits, request.getMinAngularSeparation())));
                // Move the startChunkTime
                startChunkTime = endChunkTime;
            } else {
                // Chunk is startChunkTime - endComputationTime
                futures.add(service.submit(new Worker(groundStation, refOrbit, new Date(startChunkTime), new Date(endComputationTime), targetOrbits, request.getMinAngularSeparation())));
                // Move the startChunkTime
                startChunkTime = endComputationTime;
            }
        }
        System.out.println(futures.size() + " chunks prepared for analysis");
        */
        // End chunk based division

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
                // TODO: Change Worker to FutureTask and remember it instead of futures
                futures.add(service.submit(new Worker(groundStation, refOrbit, currentTime, currentEndTime, Collections.singletonList(tOrbit), request.getMinAngularSeparation(), request.getIntervalPeriod())));
            }
            currentTime = new Date(currentEndTime.getTime() + 1);
        }
        System.out.println("Items to process: " + futures.size());

        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        events = new LinkedList<>();
        long progress = 0;
        for(Future<List<CollinearityEvent>> f : futures) {
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
            }
            ++progress;
            monitor.progress(progress, futures.size(), "TODO with future task");
        }

        // Compute progress information:
        // total is <number of points> x <number of target orbits + 1>
        // long numPoints = ((request.getEndTime().getTime() - request.getStartTime().getTime()) / ORBIT_POINT_INTERVAL) * (targetOrbits.size() + 1);
        // long currentProgress = 0;

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
        ps.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s", "Ground Station", "Time", "Reference Orbit", "Reference Point AZ", "Reference Point EL", "Target Orbit", "Target Point AZ", "Target Point EL", "Angular Separation (degrees)" );
        ps.println();
        // Write events
        for(CollinearityEvent e : events) {
            ps.print(e.toCSV());
            ps.println();
        }
        // Close
        ps.close();
    }

    private static class Worker implements Callable<List<CollinearityEvent>> {
        private final GroundStation groundStation;
        private final Orbit referenceOrbit;
        private final Date start;
        private final Date end;
        private final List<Orbit> targetOrbits;
        private final double minAngularSeparation;
        private final int pointInterval;
        public Worker(GroundStation groundStation, Orbit referenceOrbit, Date start, Date end, List<Orbit> targetOrbits, double minAngularSeparation, int pointInterval) {
            this.groundStation = groundStation.copy();
            this.groundStation.setReducedProcessing();
            this.groundStation.setConfiguration(groundStation.getConfiguration());
            this.referenceOrbit = referenceOrbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.start = start;
            this.end = end;
            this.targetOrbits = targetOrbits.stream().map(Orbit::copy).collect(Collectors.toList());
            this.targetOrbits.forEach(o -> o.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration()));
            this.minAngularSeparation = minAngularSeparation;
            this.pointInterval = pointInterval * 1000;
        }

        @Override
        public List<CollinearityEvent> call() throws Exception {
            // Register the ground station to the orbits
            this.targetOrbits.forEach(o -> o.addListener(this.groundStation));
            this.referenceOrbit.addListener(this.groundStation);
            // Instantiate the data collector
            CollinearityDataCollector dataCollector = new CollinearityDataCollector(this.referenceOrbit, this.minAngularSeparation);
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

    private static class CollinearityDataCollector implements IGroundStationListener {

        private final List<CollinearityEvent> events = new LinkedList<>();
        private final Orbit referenceOrbit;
        private final double minAngularSeparation; // in degrees
        private Date currentDate = null;
        private TrackPoint referenceOrbitCurrentPosition = null;
        private Vector3D groundStationPoint;
        private Vector3D currentReferenceVector = null;

        public CollinearityDataCollector(Orbit referenceOrbit, double minAngularSeparation) {
            this.referenceOrbit = referenceOrbit;
            this.minAngularSeparation = minAngularSeparation;
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
                // Reference s/c: save info ...
                this.currentDate = point.getTime();
                this.referenceOrbitCurrentPosition = point;
                // ... and compute the reference (normalized) vector
                Vector3D scPositionPoint = point.getSpacecraftPosition().getPositionVector(); // S/C in earth coordinates
                this.currentReferenceVector = new Vector3D(
                        scPositionPoint.getX() - this.groundStationPoint.getX(),
                        scPositionPoint.getY() - this.groundStationPoint.getY(),
                        scPositionPoint.getZ() - this.groundStationPoint.getZ()).normalize();
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
                // Time discrepancy --> return
                if(Math.abs(this.currentDate.getTime() - point.getTime().getTime()) > 1000) {
                    return;
                }
                // If we are here, it means that both satellites are in visibility --> Compute angular separation:
                // acos(dot product of normalised position vectors wrt ground station)
                Vector3D scPositionPoint = point.getSpacecraftPosition().getPositionVector(); // S/C in earth coordinates
                Vector3D targetVector = new Vector3D(
                        scPositionPoint.getX() - this.groundStationPoint.getX(),
                        scPositionPoint.getY() - this.groundStationPoint.getY(),
                        scPositionPoint.getZ() - this.groundStationPoint.getZ()).normalize();
                double result = this.currentReferenceVector.dotProduct(targetVector);
                double angularSeparation = Math.abs(Math.acos(result));
                angularSeparation = Math.toDegrees(angularSeparation);
                if(angularSeparation <= this.minAngularSeparation) {
                    CollinearityEvent event = new CollinearityEvent(groundStation, this.referenceOrbit, orbit, point.getTime().toInstant(), this.referenceOrbitCurrentPosition, point, angularSeparation);
                    System.out.println("Collinearity event detected: " + event.toCSV());
                    // Create collinearity event
                    this.events.add(event);
                }
            }
        }
    }
}
