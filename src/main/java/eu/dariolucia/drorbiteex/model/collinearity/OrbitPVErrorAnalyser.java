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

import eu.dariolucia.drorbiteex.model.orbit.*;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class OrbitPVErrorAnalyser {

    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static Map<String, List<ErrorPoint>> analyse(OrbitPVErrorAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }
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

        ExecutorService service = Executors.newFixedThreadPool(1, (r) -> {
            Thread t = new Thread(r, "Orbit Position Velocity Error Analyser Task");
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
            Worker chunkWorker = new Worker(refOrbit, t, request, monitor, progressCounter, maxProgress);
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
        private final Orbit referenceOrbit;
        private final Date start;
        private final Date end;
        private final Orbit targetOrbit;
        private final int pointInterval;
        private final ITaskProgressMonitor monitor;
        private final AtomicLong currentProgress;
        private final long maxProgress;

        public Worker(Orbit referenceOrbit, Orbit targetOrbit,
                      OrbitPVErrorAnalysisRequest request, ITaskProgressMonitor monitor, AtomicLong currentProgress, long maxProgress) {
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
            try {
                // Instantiate the data collector
                DataCollector dataCollector = new DataCollector();
                // Register the data collector to the orbits
                this.targetOrbit.addListener(dataCollector);
                this.referenceOrbit.addListener(dataCollector);

                // Iterate from start to end
                monitor.progress(currentProgress.get(), maxProgress, toString());
                Date currentDate = start;
                while (currentDate.before(end) && !monitor.isCancelled()) {
                    // Set the time in the data collector
                    dataCollector.setCurrentTime(currentDate);
                    dataCollector.setReferenceOrbitInProcessing();
                    // Propagate the reference orbit
                    referenceOrbit.updateOrbitTime(currentDate, false);
                    dataCollector.setTargetOrbitInProcessing();
                    // Propagate the target orbit, compute the error
                    targetOrbit.updateOrbitTime(currentDate, false);
                    // Next step
                    currentDate = new Date(currentDate.getTime() + this.pointInterval);
                    monitor.progress(currentProgress.incrementAndGet(), maxProgress, toString());
                }
                // Get the events
                return dataCollector.getData();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private static class DataCollector implements IOrbitListener {
        private final List<ErrorPoint> data = new LinkedList<>();
        private Date currentDate = null;
        private SpacecraftPosition referenceOrbitCurrentPosition = null;

        private boolean refInProgress = false;

        public List<ErrorPoint> getData() {
            return data;
        }

        private ErrorPoint calculateError(Date currentDate, SpacecraftPosition reference, SpacecraftPosition target) {
            double pError = reference.getSpacecraftState().getPVCoordinates(EarthReferenceUtils.getITRF()).getPosition().distance(target.getSpacecraftState().getPVCoordinates(EarthReferenceUtils.getITRF()).getPosition());
            double vError = reference.getSpacecraftState().getPVCoordinates(EarthReferenceUtils.getITRF()).getVelocity().distance(target.getSpacecraftState().getPVCoordinates(EarthReferenceUtils.getITRF()).getVelocity());
            return new ErrorPoint(currentDate.toInstant(), pError, vError);
        }

        public void setCurrentTime(Date currentDate) {
            this.currentDate = currentDate;
        }

        @Override
        public void orbitAdded(OrbitManager manager, Orbit orbit) {
            // Nothing to do
        }

        @Override
        public void orbitRemoved(OrbitManager manager, Orbit orbit) {
            // Nothing to do
        }

        @Override
        public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
            // Nothing to do
        }

        @Override
        public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
            if(currentPosition == null) {
                return;
            }
            // Compute s/c position data
            if(this.refInProgress) {
                // Reference s/c: save tracking point info (can be null)
                this.referenceOrbitCurrentPosition = currentPosition;
            } else {
                // Target s/c: calculate error
                // Time discrepancy: > 1 second --> return
                if(Math.abs(this.currentDate.getTime() - currentPosition.getTime().getTime()) > 1000) {
                    return;
                }
                // Reference is null, this is an error, should not happen

                // If we are here, it means that we can compute the PV error
                ErrorPoint event = calculateError(this.currentDate, this.referenceOrbitCurrentPosition, currentPosition);
                // Add error point
                this.data.add(event);
                // Clear up current date
                this.currentDate = null;
            }
        }

        public void setReferenceOrbitInProcessing() {
            this.refInProgress = true;
        }

        public void setTargetOrbitInProcessing() {
            this.refInProgress = false;
        }
    }
}
