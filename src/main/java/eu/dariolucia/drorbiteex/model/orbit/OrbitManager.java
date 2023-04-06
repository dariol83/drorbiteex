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

package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.oem.OemExporterProcess;
import eu.dariolucia.drorbiteex.model.oem.OemGenerationRequest;
import eu.dariolucia.drorbiteex.model.tle.TleExporterProcess;
import eu.dariolucia.drorbiteex.model.tle.TleGenerationRequest;
import eu.dariolucia.drorbiteex.model.tle.TleUtils;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrbitManager {

    private static final Logger LOGGER = Logger.getLogger(OrbitManager.class.getName());

    private final Map<UUID, Orbit> orbits = new ConcurrentHashMap<>();

    private final List<IOrbitListener> listeners = new CopyOnWriteArrayList<>();

    private volatile Date lastReferenceTime = null;

    private final OrbitParameterConfiguration configuration = new OrbitParameterConfiguration();

    public void initialise(InputStream inputStream) throws IOException {
        OrbitConfiguration oc = OrbitConfiguration.load(inputStream);
        if(oc.getConfiguration() != null) {
            configuration.update(oc.getConfiguration());
        }
        for(Orbit orbit : oc.getOrbits()) {
            registerOrbit(orbit);
        }
    }

    public void persist(OutputStream outputStream) throws IOException {
        OrbitConfiguration oc = new OrbitConfiguration();
        oc.setOrbits(getOrbits());
        oc.setConfiguration(this.configuration);
        OrbitConfiguration.save(oc, outputStream);
        outputStream.flush();
    }

    public void newOrbit(String code, String name, String color, boolean visibility, IOrbitModel model) {
        // Create orbit
        Orbit orbit = new Orbit(UUID.randomUUID(), code, name, color, visibility, model);
        registerOrbit(orbit);
    }

    private void registerOrbit(Orbit orbit) {
        orbit.setOrbitConfiguration(this.configuration);
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

    public void clearListeners() {
        this.listeners.clear();
    }

    public Orbit getOrbit(UUID id) {
        return this.orbits.get(id);
    }

    public List<Orbit> getOrbits() {
        List<Orbit> orbitList = new LinkedList<>(this.orbits.values());
        Collections.sort(orbitList);
        return orbitList;
    }

    public void refresh() {
        updateOrbitTime(this.lastReferenceTime, true);
    }

    public void updateOrbitTime(Date time, boolean forceUpdate) {
        if(LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, String.format("Updating orbit time to %s, force update %s", time, forceUpdate));
        }
        this.lastReferenceTime = time;
        this.listeners.forEach(o -> o.startOrbitTimeUpdate(time, forceUpdate));
        AtomicLong currentStep = new AtomicLong(0);
        long totalSteps = this.orbits.values().size();
        for(Orbit ob : this.orbits.values()) {
            try {
                ob.updateOrbitTime(time, forceUpdate);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error when propagating orbit for " + ob.getName(), e);
            }
            long currentProgress = currentStep.incrementAndGet();
            this.listeners.forEach(o -> o.progressOrbitTimeUpdate(time, forceUpdate, currentProgress, totalSteps));
        }
        this.listeners.forEach(o -> o.endOrbitTimeUpdate(time, forceUpdate));
    }

    public OrbitParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void updateConfiguration(OrbitParameterConfiguration props) {
        this.configuration.update(props);
        for(Orbit o : this.orbits.values()) {
            o.setOrbitConfiguration(this.configuration);
        }
        refresh();
    }
}
