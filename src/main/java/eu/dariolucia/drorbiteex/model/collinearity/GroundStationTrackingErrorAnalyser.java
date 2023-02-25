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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GroundStationTrackingErrorAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static List<TrackingErrorPoint> analyse(GroundStationTrackingErrorAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
        GroundStation groundStation = request.getGroundStation();
        Orbit refOrbit = request.getReferenceOrbit();
        Orbit targetOrbit = request.getReferenceOrbit();

        // Set configuration to all orbits
        OrbitParameterConfiguration orbitConf = refOrbit.getOrbitConfiguration().copy();
        // Never recompute orbit and do not propagate every time
        orbitConf.setRecomputeFullDataInterval(Integer.MAX_VALUE);
        orbitConf.setAfterPropagationSteps(0);
        orbitConf.setBeforePropagationSteps(0);
        targetOrbit.setOrbitConfiguration(orbitConf);
        refOrbit.setOrbitConfiguration(orbitConf);

        ExecutorService service = Executors.newFixedThreadPool(1, (r) -> {
            Thread t = new Thread(r, "Ground Station Tracking Error Analyser Task");
            t.setDaemon(true);
            return t;
        });
        Worker chunkWorker = new Worker(groundStation, refOrbit, targetOrbit, request, monitor);
        Future<List<TrackingErrorPoint>> futureTask = service.submit(chunkWorker);
        // Shutdown the executor
        service.shutdown();
        // Get the results of the futures
        try {
            return futureTask.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            System.gc();
        }
    }

    private static class Worker implements Callable<List<TrackingErrorPoint>> {
        private final GroundStation groundStation;
        private final Orbit referenceOrbit;
        private final Date start;
        private final Date end;
        private final Orbit targetOrbit;
        private final int pointInterval;
        private final ITaskProgressMonitor monitor;

        public Worker(GroundStation groundStation, Orbit referenceOrbit, Orbit targetOrbit,
                      GroundStationTrackingErrorAnalysisRequest request, ITaskProgressMonitor monitor) {
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
        }

        @Override
        public String toString() {
            return TimeUtils.formatDate(start) + " - " + TimeUtils.formatDate(end) + ": " + targetOrbit + " <--> " + referenceOrbit;
        }

        @Override
        public List<TrackingErrorPoint> call() {
            // Register the ground station to the orbits
            this.targetOrbit.addListener(this.groundStation);
            this.referenceOrbit.addListener(this.groundStation);
            // Instantiate the data collector
            DataCollector dataCollector = new DataCollector(this.referenceOrbit);
            this.groundStation.addListener(dataCollector);
            // Iterate from start to end
            long totalProgress = end.getTime() - start.getTime();
            long currentProgress = 0;
            monitor.progress(currentProgress, totalProgress, toString());
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
                currentProgress += this.pointInterval;
                monitor.progress(currentProgress, totalProgress, toString());
            }
            // Get the events
            return dataCollector.getEvents();
        }
    }

    private static class DataCollector implements IGroundStationListener {
        private final List<TrackingErrorPoint> events = new LinkedList<>();
        private final Orbit referenceOrbit;
        private Date currentDate = null;
        private TrackPoint referenceOrbitCurrentPosition = null;
        private Vector3D groundStationPoint;

        public DataCollector(Orbit referenceOrbit) {
            this.referenceOrbit = referenceOrbit;
        }

        public List<TrackingErrorPoint> getEvents() {
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
                // Reference s/c: save tracking point info (can be null)
                this.referenceOrbitCurrentPosition = point;
            } else {
                // Target s/c: calculate error
                // Elevation of reference orbit < 0 && elevation of target orbit < 0 --> return empty point, none of the two orbits is in visibility
                if((this.referenceOrbitCurrentPosition == null || this.referenceOrbitCurrentPosition.getElevation() < 0) &&
                        (point == null || point.getElevation() < 0)) {
                    this.events.add(TrackingErrorPoint.noVisibility(currentDate.toInstant()));
                    return;
                }
                // Time discrepancy --> return
                if(point != null && Math.abs(this.currentDate.getTime() - point.getTime().getTime()) > 1000) {
                    return;
                }
                // If we are here, it means that at least one satellite is in visibility: compute error
                TrackingErrorPoint event = calculateError(this.currentDate, this.referenceOrbitCurrentPosition, point);
                // Create collinearity event
                this.events.add(event);
            }
        }

        private TrackingErrorPoint calculateError(Date currentDate, TrackPoint reference, TrackPoint target) {
            if(reference == null) {
                return new TrackingErrorPoint(currentDate.toInstant(),
                        -1, // Azimuth error cannot be computed
                        Math.abs(target.getElevation()));
            } else if(target == null) {
                return new TrackingErrorPoint(currentDate.toInstant(),
                        -1, // Azimuth error cannot be computed
                        Math.abs(reference.getElevation()));
            } else {
                return new TrackingErrorPoint(currentDate.toInstant(),
                        computeAzimuthError(reference.getAzimuth(), target.getAzimuth()),
                        Math.abs(reference.getElevation() - target.getElevation()));
            }
        }

        private double computeAzimuthError(double refAz, double tarAz) {
            double res = Math.abs(refAz - tarAz);
            return res >= 180 ? 360 - res : res;
        }

        public void setCurrentTime(Date currentDate) {
            this.currentDate = currentDate;
        }
    }
}
