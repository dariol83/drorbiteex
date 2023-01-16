package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CollinearityAnalyser {

    private static final long ORBIT_POINT_INTERVAL = 5000; // in milliseconds
    private static final long ORBIT_COMPUTATION_CHUNK = 100; // max number of ORBIT_POINT_INTERVAL periods computed

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        this.cancelled.set(true);
    }
    public List<CollinearityEvent> analyse(CollinearityAnalysisRequest request, ICollinearityProgressMonitor monitor) throws IOException {
        if(cancelled.get()) {
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
        if(cancelled.get()) {
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
        // Never recompute orbit
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        for(Orbit o : targetOrbits) {
            o.setOrbitConfiguration(orbitConf);
        }

        // One chunk, one thread: use thread pool
        int threadsToUse = Runtime.getRuntime().availableProcessors()/2;
        if(threadsToUse <= 0) {
            threadsToUse = 1;
        }
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

        // Start satellite based division
        for(Orbit tOrbit : targetOrbits) {
            if(cancelled.get()) {
                service.shutdownNow();
                return null;
            }
            futures.add(service.submit(new Worker(groundStation, refOrbit, request.getStartTime(), request.getEndTime(), Collections.singletonList(tOrbit), request.getMinAngularSeparation())));
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
                if(cancelled.get()) {
                    service.shutdownNow();
                    return null;
                }
                events.addAll(subEventList);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
            ++progress;
            if(monitor != null) {
                monitor.progress(progress, futures.size(), "Analysis for " + targetOrbits.get((int) (progress - 1)).getName() + " completed");
            }
        }

        // Compute progress information:
        // total is <number of points> x <number of target orbits + 1>
        // long numPoints = ((request.getEndTime().getTime() - request.getStartTime().getTime()) / ORBIT_POINT_INTERVAL) * (targetOrbits.size() + 1);
        // long currentProgress = 0;

        // Return the events
        return events;
    }

    public void generateCSV(String filePath, List<CollinearityEvent> events) throws IOException {
        if(cancelled.get()) {
            return;
        }
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

        public Worker(GroundStation groundStation, Orbit referenceOrbit, Date start, Date end, List<Orbit> targetOrbits, double minAngularSeparation) {
            this.groundStation = groundStation.copy();
            this.groundStation.setConfiguration(groundStation.getConfiguration());
            this.referenceOrbit = referenceOrbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.start = start;
            this.end = end;
            this.targetOrbits = targetOrbits.stream().map(Orbit::copy).collect(Collectors.toList());
            this.targetOrbits.forEach(o -> o.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration()));
            this.minAngularSeparation = minAngularSeparation;
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
                // Progress
                // ++currentProgress;
                // if(monitor != null) {
                //     monitor.progress(currentProgress, numPoints, currentDate + " - " + refOrbit.getName());
                // }
                // Propagate the target orbits
                for(Orbit o : targetOrbits) {
                    o.updateOrbitTime(currentDate, false);
                    // Progress
                    // ++currentProgress;
                    // if(monitor != null) {
                    //    monitor.progress(currentProgress, numPoints, currentDate + " - " + o.getName());
                    // }
                }
                // Next step
                currentDate = new Date(currentDate.getTime() + ORBIT_POINT_INTERVAL);
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
