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

package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleExporterProcess {

    private final OrbitParameterConfiguration configuration;
    private final ScheduleGenerationRequest request;

    public ScheduleExporterProcess(OrbitParameterConfiguration configuration, ScheduleGenerationRequest request) {
        this.configuration = configuration;
        this.request = request;
    }

    public String exportSchedule(ITaskProgressMonitor monitor) throws IOException {
        // For all the provided orbits, get a copy of the model and compute the visibility windows between the dates
        IScheduleExporter externalExporter = ScheduleExporterRegistry.instance().getExporter(request.getExporterToUse());
        // Compute generation time and check filename
        Date genDate = new Date();
        CcsdsSimpleScheduleExporter exporter = null;
        String generatedFile = null;
        if(request.getFilePath() != null) {
            exporter = new CcsdsSimpleScheduleExporter(request.getFilePath(), externalExporter, request);
            generatedFile = request.getFilePath();
        } else {
            String folder = request.getFolderPath();
            IScheduleNameGenerator generator = ScheduleExporterRegistry.instance().getNameGenerator(request.getGeneratorToUse());
            folder += File.separator + generator.generateFileName(request, genDate);
            exporter = new CcsdsSimpleScheduleExporter(folder, externalExporter, request);
            generatedFile = folder;
        }
        exporter.writeHeader(request, genDate);
        // Go for passes - Compute everything and put it in a map
        Map<Orbit, List<VisibilityWindow>> orbit2passesMap = new LinkedHashMap<>();
        int progress = 0;
        for(Orbit o : request.getOrbits()) {
            List<VisibilityWindow> passes = computePasses(request.getGroundStation(), o, request.getStartTime(), request.getEndTime());
            if(monitor != null && monitor.isCancelled()) {
                return null;
            }
            orbit2passesMap.put(o, passes);
        }
        // Now write the file
        for(Orbit o : request.getOrbits()) {
            List<VisibilityWindow> passes = orbit2passesMap.get(o);
            exporter.writeScheduledPackage(request, request.getGroundStation(), o, passes, Collections.unmodifiableMap(orbit2passesMap));
            ++progress;
            if(monitor != null) {
                monitor.progress(progress, request.getOrbits().size(), "Processed " + o.getName());
            }
        }
        exporter.close();
        return generatedFile;
    }

    private List<VisibilityWindow> computePasses(GroundStation groundStation, Orbit orbit, Date startTime, Date endTime) {
        // Clone the orbit and the ground station
        Orbit clonedOrbit = new Orbit(orbit.getId(), orbit.getCode(), orbit.getName(), orbit.getColor(), orbit.isVisible(), orbit.getModel().copy());
        clonedOrbit.setOrbitConfiguration(this.configuration);
        GroundStation clonedStation = new GroundStation(groundStation.getId(), groundStation.getCode(), groundStation.getName(), groundStation.getSite(), groundStation.getDescription(), groundStation.getColor(),
                groundStation.isVisible(), groundStation.getLatitude(), groundStation.getLongitude(), groundStation.getHeight(),
                groundStation.getMask() != null ? groundStation.getMask().copy() : null);
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
}
