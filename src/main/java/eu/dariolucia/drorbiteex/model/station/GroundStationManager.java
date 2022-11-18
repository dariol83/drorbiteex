package eu.dariolucia.drorbiteex.model.station;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroundStationManager {

    private final Map<UUID, GroundStation> groundStations = new ConcurrentHashMap<>();

    private final List<IGroundStationListener> listeners = new CopyOnWriteArrayList<>();

    public void initialise(InputStream inputStream) throws IOException {
        GroundStationConfiguration oc = GroundStationConfiguration.load(inputStream);
        for(GroundStation groundStation : oc.getGroundStations()) {
            registerStation(groundStation);
        }
    }

    public void persist(OutputStream outputStream) throws IOException {
        GroundStationConfiguration oc = new GroundStationConfiguration();
        oc.setGroundStations(new LinkedList<>(this.groundStations.values()));
        GroundStationConfiguration.save(oc, outputStream);
        outputStream.flush();
    }

    public void newGroundStation(String code, String name, String site, String description, String color, boolean visibility, double latitude, double longitude, double height) {
        // Create station
        GroundStation station = new GroundStation(UUID.randomUUID(), code, name, site, description, color, visibility, latitude, longitude, height);
        registerStation(station);
    }

    private void registerStation(GroundStation st) {
        // Register station
        this.groundStations.put(st.getId(), st);
        // Add observers to new orbit
        this.listeners.forEach(st::addListener);
        // Notify for new orbit
        notifyNewStationAdded(st);
    }

    private void notifyNewStationAdded(GroundStation st) {
        this.listeners.forEach(o -> o.groundStationAdded(this, st));
    }

    public void removeGroundStation(UUID id) {
        // GroundStation lookup
        GroundStation toRemove = this.groundStations.remove(id);
        if(toRemove != null) {
            // Remove listeners if existing
            toRemove.clearListeners();
            // Notify orbit deleted
            notifyStationRemoved(toRemove);
        }
    }

    private void notifyStationRemoved(GroundStation orbit) {
        this.listeners.forEach(o -> o.groundStationRemoved(this, orbit));
    }

    public void addListener(IGroundStationListener l) {
        // Add to list
        this.listeners.add(l);
        // Add to all groundStations
        this.groundStations.values().forEach(o -> o.addListener(l));
    }

    public void removeListener(IGroundStationListener l) {
        // Remove from list
        this.listeners.remove(l);
        // Remove from all groundStations
        this.groundStations.values().forEach(o -> o.removeListener(l));
    }

    public void clearListeners(IGroundStationListener l) {
        this.listeners.clear();
    }

    public GroundStation getGroundStation(UUID id) {
        return this.groundStations.get(id);
    }

    public Map<UUID, GroundStation> getGroundStations() {
        return Map.copyOf(this.groundStations);
    }
}
