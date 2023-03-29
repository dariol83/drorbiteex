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

package eu.dariolucia.drorbiteex.model.oem;

import eu.dariolucia.drorbiteex.model.orbit.IOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.EphemerisWriter;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.ndm.odm.oem.OemMetadata;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.general.OrekitEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OemExporterProcess {

    public String exportOem(OemGenerationRequest request, ITaskProgressMonitor monitor) throws IOException {
        Orbit targetOrbit = request.getOrbit();
        if(targetOrbit == null) {
            throw new IllegalArgumentException("Orbit not set");
        }

        AbsoluteDate startDate = TimeUtils.toAbsoluteDate(request.getStartTime());
        AbsoluteDate endDate = TimeUtils.toAbsoluteDate(request.getEndTime());

        // Get copy of model propagator
        IOrbitModel model = targetOrbit.getModel().copy();
        Propagator p = model.getPropagator();

        // Check https://forum.orekit.org/t/spacececraftstate-in-wgs84/1813

        // Propagate from startDate
        List<SpacecraftState> states = new ArrayList<>();
        SpacecraftState ss1 = p.propagate(startDate);
        states.add(convert(ss1, request.getFrame()));

        // Move propagation by steps of periodSeconds - compute progress
        long totalProgress = 0;
        {
            AbsoluteDate currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                currentDate = currentDate.shiftedBy(request.getPeriodSeconds());
                ++totalProgress;
            }
        }
        totalProgress += 1; // for file generation
        //
        long progress = 0;
        AbsoluteDate currentDate = startDate;
        while(currentDate.isBefore(endDate)) {
            currentDate = currentDate.shiftedBy(request.getPeriodSeconds());
            SpacecraftState ss = p.propagate(currentDate);
            states.add(convert(ss, request.getFrame()));
            if(monitor != null && monitor.isCancelled()) {
                return null;
            }
            if(monitor != null) {
                monitor.progress(progress, totalProgress, "Date: " + TimeUtils.formatDate(currentDate.toDate(TimeScalesFactory.getUTC())));
            }
            ++progress;
        }

        if(monitor != null && monitor.isCancelled()) {
            return null;
        }

        Date generationDate = new Date();
        String generatedFile;
        if(request.getFile() != null) {
            generatedFile = request.getFile();
        } else {
            String folder = request.getFolder();
            IOemNameGenerator generator = OemExporterRegistry.instance().getNameGenerator(request.getNameGenerator());
            folder += File.separator + generator.generateFileName(request, generationDate);
            generatedFile = folder;
        }

        // Write OEM
        OrekitEphemerisFile ephemerisFile = new OrekitEphemerisFile();
        OrekitEphemerisFile.OrekitSatelliteEphemeris satellite = ephemerisFile.addSatellite(request.getCode());
        satellite.addNewSegment(states, 7);

        OemMetadata template = new OemMetadata(7);
        template.setTimeSystem(TimeSystem.UTC);
        template.setObjectID(request.getCode());
        template.setObjectName(request.getName());
        template.setCenter(new BodyFacade("EARTH", CelestialBodyFactory.getCelestialBodies().getEarth()));
        template.setReferenceFrame(FrameFacade.map(request.getFrame()));
        template.setInterpolationMethod(InterpolationMethod.LAGRANGE);
        template.setInterpolationDegree(7);
        template.setUseableStartTime(startDate);
        template.setUseableStopTime(states.get(states.size() - 1).getDate());

        Header header = new Header(2);
        header.setOriginator("Dr Orbiteex");
        header.setCreationDate(TimeUtils.toAbsoluteDate(generationDate));
        EphemerisWriter writer;
        if(request.getFormat() == FileFormat.XML) {
            header.setFormatVersion(2.0);
            //
            writer = new DrOrbiteexEphemerisWriter(new WriterBuilder().buildOemWriter(),
                    header, template, request.getFormat(), "dummy", 60);
        } else {
            writer = new EphemerisWriter(new WriterBuilder().buildOemWriter(),
                    header, template, request.getFormat(), "dummy", 60);
        }
        if(monitor != null && monitor.isCancelled()) {
            return null;
        }
        writer.write(generatedFile, ephemerisFile);

        // Post process
        if(request.getPostProcessor() != null) {
            IOemPostProcessor processor = OemExporterRegistry.instance().getPostProcessor(request.getPostProcessor());
            if(processor != null) {
                processor.postProcess(generatedFile, request, generationDate);
            }
        }
        if(monitor != null) {
            monitor.progress(totalProgress, totalProgress, "File generated: " + generatedFile);
        }

        return generatedFile;
    }

    private SpacecraftState convert(SpacecraftState ss, Frame targetFrame) {
        if(ss.getFrame().equals(targetFrame)) {
            return ss;
        } else {
            // Spacecraft conversion implies:
            // 1. Attitude conversion
            // 2. Position conversion
            TimeStampedPVCoordinates tsPV = ss.getPVCoordinates(targetFrame);
            AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(targetFrame, tsPV);
            //
            Attitude newAttitude = ss.getAttitude().withReferenceFrame(targetFrame);
            //
            return new SpacecraftState(absolutePVCoordinates, newAttitude, ss.getMass());
        }
    }
}
