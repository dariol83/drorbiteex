/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
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
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrbitManager {

    private static final Logger LOGGER = Logger.getLogger(OrbitManager.class.getName());

    private final Map<UUID, Orbit> orbits = new ConcurrentHashMap<>();

    private final List<IOrbitListener> listeners = new CopyOnWriteArrayList<>();

    private volatile Date lastReferenceTime = null;

    public void initialise(InputStream inputStream) throws IOException {
        OrbitConfiguration oc = OrbitConfiguration.load(inputStream);
        for(Orbit orbit : oc.getOrbits()) {
            registerOrbit(orbit);
        }
    }

    public void persist(OutputStream outputStream) throws IOException {
        OrbitConfiguration oc = new OrbitConfiguration();
        oc.setOrbits(new LinkedList<>(this.orbits.values()));
        OrbitConfiguration.save(oc, outputStream);
        outputStream.flush();
    }

    public void newOrbit(String code, String name, String color, boolean visibility, IOrbitModel model) {
        // Create orbit
        Orbit orbit = new Orbit(UUID.randomUUID(), code, name, color, visibility, model);
        registerOrbit(orbit);
    }

    private void registerOrbit(Orbit orbit) {
        // Register orbit
        this.orbits.put(orbit.getId(), orbit);
        // Add observers to new orbit
        this.listeners.forEach(orbit::addListener);
        // Notify for new orbit
        notifyNewOrbitAdded(orbit);
    }

    private void notifyNewOrbitAdded(Orbit orbit) {
        this.listeners.forEach(o -> o.orbitAdded(this, orbit));
    }

    public void removeOrbit(UUID id) {
        // Orbit lookup
        Orbit toRemove = this.orbits.remove(id);
        if(toRemove != null) {
            // Remove listeners if existing
            toRemove.clearListeners();
            // Notify orbit deleted
            notifyOrbitRemoved(toRemove);
        }
    }

    private void notifyOrbitRemoved(Orbit orbit) {
        this.listeners.forEach(o -> o.orbitRemoved(this, orbit));
    }

    public void addListener(IOrbitListener l) {
        // Add to list
        this.listeners.add(l);
        // Add to all orbits
        this.orbits.values().forEach(o -> o.addListener(l));
    }

    public void removeListener(IOrbitListener l) {
        // Remove from list
        this.listeners.remove(l);
        // Remove from all orbits
        this.orbits.values().forEach(o -> o.removeListener(l));
    }

    public void clearListeners(IOrbitListener l) {
        this.listeners.clear();
    }

    public Orbit getOrbit(UUID id) {
        return this.orbits.get(id);
    }

    public Map<UUID, Orbit> getOrbits() {
        return Map.copyOf(this.orbits);
    }

    public void refresh() {
        updateOrbitTime(this.lastReferenceTime, true);
    }

    public void updateOrbitTime(Date time, boolean forceUpdate) {
        LOGGER.log(Level.FINE, "Updating orbit time to " + time + ", force update " + forceUpdate);
        this.lastReferenceTime = time;
        this.listeners.forEach(o -> o.startOrbitTimeUpdate(time, forceUpdate));
        for(Orbit ob : this.orbits.values()) {
            try {
                ob.updateOrbitTime(time, forceUpdate);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error when propagating orbit for " + ob.getName(), e);
            }
        }
        this.listeners.forEach(o -> o.endOrbitTimeUpdate(time, forceUpdate));
    }

    public void exportOem(UUID id, String code, String name, Date startTime, Date endTime, int periodSeconds, String file, Frame targetFrame, FileFormat format) throws IOException {
        Orbit targetOrbit = getOrbit(id);
        if(targetOrbit == null) {
            throw new IllegalArgumentException("Orbit ID " + id + " cannot be found");
        }

        AbsoluteDate startDate = TimeUtils.toAbsoluteDate(startTime);
        AbsoluteDate endDate = TimeUtils.toAbsoluteDate(endTime);

        // Get copy of model propagator
        IOrbitModel model = targetOrbit.getModel().copy();
        Propagator p = model.getPropagator();

        // Check https://forum.orekit.org/t/spacececraftstate-in-wgs84/1813

        // Propagate from startDate
        List<SpacecraftState> states = new ArrayList<>();
        SpacecraftState ss1 = p.propagate(startDate);
        states.add(convert(ss1, targetFrame));

        // Move propagation by steps of periodSeconds
        AbsoluteDate currentDate = startDate;
        while(currentDate.isBefore(endDate)) {
            currentDate = currentDate.shiftedBy(periodSeconds);
            SpacecraftState ss = p.propagate(currentDate);
            // states.add(ss.getPVCoordinates(targetFrame));
            states.add(convert(ss, targetFrame));
        }
        // Write OEM
        OrekitEphemerisFile ephemerisFile = new OrekitEphemerisFile();
        OrekitEphemerisFile.OrekitSatelliteEphemeris satellite = ephemerisFile.addSatellite(code);
        // OrekitEphemerisFile.OrekitEphemerisSegment segment = new OrekitEphemerisFile.OrekitEphemerisSegment(states,
        //        targetFrame, CelestialBodyFactory.getCelestialBodies().getEarth().getGM(), 7);
        satellite.addNewSegment(states, 7);

        OemMetadata template = new OemMetadata(7);
        template.setTimeSystem(TimeSystem.UTC);
        template.setObjectID(code);
        template.setObjectName(name);
        template.setCenter(new BodyFacade("EARTH", CelestialBodyFactory.getCelestialBodies().getEarth()));
        template.setReferenceFrame(FrameFacade.map(targetFrame));
        template.setInterpolationMethod(InterpolationMethod.LAGRANGE);
        template.setInterpolationDegree(7);
        template.setUseableStartTime(startDate);
        template.setUseableStopTime(states.get(states.size() - 1).getDate());

        Header header = new Header(2);
        header.setOriginator("Dr Orbiteex");

        //
        EphemerisWriter writer = new EphemerisWriter(new WriterBuilder().buildOemWriter(),
                header, template, format, "dummy", 60);
        writer.write(file, ephemerisFile);
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
            TimeStampedPVCoordinates orbitPVinTargetFrame = ss.getOrbit().getPVCoordinates(targetFrame);
            //
            Attitude newAttitude = ss.getAttitude().withReferenceFrame(targetFrame);
            //
            return new SpacecraftState(absolutePVCoordinates, newAttitude, ss.getMass());
        }
    }
}
