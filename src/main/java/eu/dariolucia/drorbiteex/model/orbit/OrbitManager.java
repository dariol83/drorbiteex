package eu.dariolucia.drorbiteex.model.orbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrbitManager {

    private final Map<UUID, Orbit> orbits = new ConcurrentHashMap<>();

    private final List<IOrbitListener> listeners = new CopyOnWriteArrayList<>();

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
}
