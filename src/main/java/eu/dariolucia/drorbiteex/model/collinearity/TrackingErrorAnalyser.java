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
import eu.dariolucia.drorbiteex.model.station.*;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class TrackingErrorAnalyser {

    public static final int ERROR_POINT_AZ_ERROR_IDX = 0;
    public static final int ERROR_POINT_EL_ERROR_IDX = 1;
    public static final int ERROR_POINT_ANGLE_ERROR_IDX = 2;

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static Map<String, List<ErrorPoint>> analyse(TrackingErrorAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        GroundStation groundStation = request.getGroundStation();
        Orbit refOrbit = request.getReferenceOrbit();
        List<Orbit> targetOrbits = request.getTargetOrbits();

        // Set configuration to all orbits
        OrbitParameterConfiguration orbitConf = refOrbit.getOrbitConfiguration().copy();
        // Never recompute orbit and do not propagate every time
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setAfterPropagationSteps(0);
        orbitConf.setBeforePropagationSteps(0);
        targetOrbits.forEach(o -> o.setOrbitConfiguration(orbitConf));
        refOrbit.setOrbitConfiguration(orbitConf);

        ExecutorService service = Executors.newSingleThreadExecutor((r) -> {
            Thread t = new Thread(r, "Ground Station Tracking Error Analyser Task");
            t.setDaemon(true);
            return t;
        });
        Map<String, List<ErrorPoint>> orbit2points = new HashMap<>();
        List<String> names = new ArrayList<>();
        List<Future<List<ErrorPoint>>> futures = new ArrayList<>();
        AtomicLong progressCounter = new AtomicLong();
        // Compute the number of total steps
        long maxProgress = 0;
        Date currentDate = request.getStartTime();
        while(currentDate.before(request.getEndTime())) {
            // Next step
            currentDate = new Date(currentDate.getTime() + request.getIntervalPeriod() * 1000L);
            ++maxProgress;
        }
        maxProgress *= targetOrbits.size();
        for(Orbit t : targetOrbits) {
            Worker chunkWorker = new Worker(groundStation, refOrbit, t, request, monitor, progressCounter, maxProgress);
            Future<List<ErrorPoint>> futureTask = service.submit(chunkWorker);
            names.add(t.getName());
            futures.add(futureTask);
        }
        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        try {
            for(int i = 0; i < futures.size(); ++i) {
                if (monitor.isCancelled()) {
                    service.shutdownNow();
                    return null;
                }
                List<ErrorPoint> data = futures.get(i).get();
                orbit2points.put(names.get(i), data);
            }
            monitor.progress(1, 1, "Done");
            return orbit2points;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            System.gc();
        }
    }

    private static class Worker implements Callable<List<ErrorPoint>> {
        private final GroundStation groundStation;
        private final Orbit referenceOrbit;
        private final Date start;
        private final Date end;
        private final Orbit targetOrbit;
        private final int pointInterval;
        private final ITaskProgressMonitor monitor;
        private final AtomicLong currentProgress;
        private final long maxProgress;

        public Worker(GroundStation groundStation, Orbit referenceOrbit, Orbit targetOrbit,
                      TrackingErrorAnalysisRequest request, ITaskProgressMonitor monitor, AtomicLong currentProgress, long maxProgress) {
            this.groundStation = groundStation.copy();
            this.groundStation.setReducedProcessing();
            this.groundStation.setConfiguration(groundStation.getConfiguration());
            this.referenceOrbit = referenceOrbit.copy();
            this.referenceOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.start = request.getStartTime();
            this.end = request.getEndTime();
            this.monitor = monitor;
            this.targetOrbit = targetOrbit.copy();
            this.targetOrbit.setOrbitConfiguration(referenceOrbit.getOrbitConfiguration());
            this.pointInterval = request.getIntervalPeriod() * 1000;
            this.currentProgress = currentProgress;
            this.maxProgress = maxProgress;
        }

        @Override
        public String toString() {
            return TimeUtils.formatDate(start) + " - " + TimeUtils.formatDate(end) + ": " + targetOrbit + " <--> " + referenceOrbit;
        }

        @Override
        public List<ErrorPoint> call() {
            // Register the ground station to the orbits
            this.targetOrbit.addListener(this.groundStation);
            this.referenceOrbit.addListener(this.groundStation);
            // Instantiate the data collector
            DataCollector dataCollector = new DataCollector(this.referenceOrbit);
            this.groundStation.addListener(dataCollector);
            // Iterate from start to end
            monitor.progress(currentProgress.get(), maxProgress, toString());
            Date currentDate = start;
            while(currentDate.before(end) && !monitor.isCancelled()) {
                // Set the time in the data collector
                dataCollector.setCurrentTime(currentDate);
                // Propagate the reference orbit
                referenceOrbit.updateOrbitTime(currentDate, false);
                // Propagate the target orbit
                targetOrbit.updateOrbitTime(currentDate, false);
                // Next step
                currentDate = new Date(currentDate.getTime() + this.pointInterval);
                monitor.progress(currentProgress.incrementAndGet(), maxProgress, toString());
            }
            // Get the events
            return dataCollector.getData();
        }
    }

    private static class DataCollector implements IGroundStationListener {
        private final List<ErrorPoint> data = new LinkedList<>();
        private final Orbit referenceOrbit;
        private Date currentDate = null;
        private TrackPoint referenceOrbitCurrentPosition = null;
        private Vector3D groundStationPoint;
        private ErrorPoint lastNoVisibilityPoint;

        public DataCollector(Orbit referenceOrbit) {
            this.referenceOrbit = referenceOrbit;
        }

        public List<ErrorPoint> getData() {
            return data;
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
                // Reference s/c: save tracking point info (can be null)
                this.referenceOrbitCurrentPosition = point;
            } else {
                // Target s/c: calculate error
                // Elevation of reference orbit < 0 && elevation of target orbit < 0 --> return empty point, none of the two orbits is in visibility
                if((this.referenceOrbitCurrentPosition == null || this.referenceOrbitCurrentPosition.getElevation() < 0) &&
                        (point == null || point.getElevation() < 0)) {
                    this.data.add(new ErrorPoint(currentDate.toInstant(), -1.0, -1.0, -1.0));
                    // Clear up current date
                    this.currentDate = null;
                    return;
                }
                // Time discrepancy --> return
                if(point != null && Math.abs(this.currentDate.getTime() - point.getTime().getTime()) > 1000) {
                    return;
                }
                // If we are here, it means that at least one satellite is in visibility: compute error
                ErrorPoint event = calculateError(this.currentDate, this.referenceOrbitCurrentPosition, point);
                // Add error point, but check first if a no-visibility for the previous step is needed
                if(!data.isEmpty()) {
                    // Get last recorded event
                    ErrorPoint lastRecorded = this.data.get(this.data.size() - 1);
                    if(this.lastNoVisibilityPoint != null && lastRecorded != this.lastNoVisibilityPoint) {
                        this.data.add(this.lastNoVisibilityPoint);
                        this.lastNoVisibilityPoint = null;
                    }
                }
                this.data.add(event);
                // Clear up current date
                this.currentDate = null;
            }
        }

        private ErrorPoint calculateError(Date currentDate, TrackPoint reference, TrackPoint target) {
            if(reference == null) {
                return new ErrorPoint(currentDate.toInstant(),
                        -1, // Azimuth error cannot be computed
                        Math.abs(target.getElevation()),
                        -1);
            } else if(target == null) {
                return new ErrorPoint(currentDate.toInstant(),
                        -1, // Azimuth error cannot be computed
                        Math.abs(reference.getElevation()),
                        -1);
            } else {
                return new ErrorPoint(currentDate.toInstant(),
                        computeAzimuthError(reference.getAzimuth(), target.getAzimuth()),
                        Math.abs(reference.getElevation() - target.getElevation()),
                        computeAngleDifference(reference, target));
            }
        }

        private double computeAngleDifference(TrackPoint reference, TrackPoint target) {
            Vector3D referenceVector = reference.getSpacecraftPosition().computeVisibilityVectorFrom(this.groundStationPoint);
            Vector3D targetVector = target.getSpacecraftPosition().computeVisibilityVectorFrom(this.groundStationPoint);
            return Math.toDegrees(Math.abs(Math.acos(referenceVector.dotProduct(targetVector))));
        }

        private double computeAzimuthError(double refAz, double tarAz) {
            double res = Math.abs(refAz - tarAz);
            return res >= 180 ? 360 - res : res;
        }

        public void setCurrentTime(Date currentDate) {
            // Current date must be null here! If not, it means no point was added
            // To avoid too many points, find a clever solution (i.e. if the previous point was already a non visibility point,
            // then do not add the point)
            if(this.currentDate != null) {
                this.lastNoVisibilityPoint = new ErrorPoint(currentDate.toInstant(), -1.0, -1.0, -1.0);
                if((this.data.isEmpty() || visible(this.data.get(this.data.size() - 1)))) {
                    this.data.add(this.lastNoVisibilityPoint);
                }
            }
            this.currentDate = currentDate;
        }

        private boolean visible(ErrorPoint point) {
            return point.getErrorAt(ERROR_POINT_AZ_ERROR_IDX) != -1 || point.getErrorAt(ERROR_POINT_EL_ERROR_IDX) != -1;
        }
    }
}
