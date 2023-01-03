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

package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.orbit.IOrbitVisibilityProcessor;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitManager;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStation implements EventHandler<ElevationDetector>, IOrbitVisibilityProcessor {

    private static final double MAX_CHECK = 60.0;
    private static final double THRESHOLD =  0.001;

    private volatile UUID id;
    private volatile String code = "";
    private volatile String name = "";
    private volatile String site = "";
    private volatile String description = "";
    private volatile String color = "0xFFFFFF";
    private volatile boolean visible = false;
    private volatile double latitude;
    private volatile double longitude;
    private volatile double height;

    // Fields for visibility window computation
    private transient final Map<Orbit, List<VisibilityWindow>> visibilityWindows = new ConcurrentHashMap<>();
    private transient final Map<Orbit, TrackPoint> currentVisibilityMap = new ConcurrentHashMap<>();
    private transient final Map<Orbit, VisibilityCircle> visibilityCircles = new ConcurrentHashMap<>();

    private transient volatile TopocentricFrame stationFrame;

    private transient volatile EventDetector eventDetector;

    private transient final List<WeakReference<IGroundStationListener>> listeners = new CopyOnWriteArrayList<>();

    // To record the update state on visibility process
    private boolean visibilityUpdateInProgress = false;

    private volatile GroundStationParameterConfiguration configuration;

    public GroundStation() {
        //
    }

    public GroundStation(UUID id, String code, String name, String site, String description, String color, boolean visible, double latitude, double longitude, double height) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.site = site;
        this.description = description;
        this.color = color;
        this.visible = visible;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
    }

    public GroundStationParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(GroundStationParameterConfiguration configuration) {
        this.configuration = configuration;
        // Recompute event detector
        recomputeData(false);
    }

    public void addListener(IGroundStationListener l) {
        this.listeners.add(new WeakReference<>(l));
    }

    public void removeListener(IGroundStationListener l) {
        this.listeners.removeIf(o -> {
            IGroundStationListener obj = o.get();
            return obj == null || obj == l;
        });
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    @XmlAttribute(required = true)
    public synchronized UUID getId() {
        return id;
    }

    private synchronized void setId(UUID id) {
        this.id = id;
    }

    @XmlAttribute
    public synchronized String getCode() {
        return code;
    }

    public synchronized void setCode(String code) {
        this.code = code;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized String getSite() {
        return site;
    }

    public synchronized void setSite(String site) {
        this.site = site;
        notifyGroundStationUpdated();
    }

    @XmlElement
    public synchronized String getDescription() {
        return description;
    }

    public synchronized void setDescription(String description) {
        this.description = description;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized String getColor() {
        return color;
    }

    public synchronized void setColor(String color) {
        this.color = color;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized boolean isVisible() {
        return visible;
    }

    public synchronized void setVisible(boolean visible) {
        if(this.visible != visible) {
            this.visible = visible;
            notifyGroundStationUpdated();
        }
    }

    @XmlAttribute
    public synchronized double getLatitude() {
        return latitude;
    }

    private synchronized void setLatitude(double latitude) {
        this.latitude = latitude;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized double getLongitude() {
        return longitude;
    }

    private synchronized void setLongitude(double longitude) {
        this.longitude = longitude;
        notifyGroundStationUpdated();
    }

    @XmlAttribute
    public synchronized double getHeight() {
        return height;
    }

    private synchronized void setHeight(double height) {
        this.height = height;
        notifyGroundStationUpdated();
    }

    @Override
    public synchronized String toString() {
        return this.code + " - " + this.name + "[" + this.latitude + ", " + this.longitude + ", " + this.height + "] - " + (visible ? "visible" : "hidden");
    }

    public synchronized void update(GroundStation gs) {
        this.code = gs.getCode();
        this.name = gs.getName();
        this.site = gs.getSite();
        this.description = gs.getDescription();
        this.color = gs.getColor();
        this.latitude = gs.getLatitude();
        this.longitude = gs.getLongitude();
        this.height = gs.getHeight();

        recomputeData(true);
    }

    private void recomputeData(boolean notify) {
        GeodeticPoint geodeticPoint = new GeodeticPoint(Math.toRadians(getLatitude()), Math.toRadians(getLongitude()), getHeight());
        this.stationFrame = new TopocentricFrame(EarthReferenceUtils.getEarthShape(), geodeticPoint, getCode());
        double gsElevation = Math.toRadians(configuration.getElevationThreshold());
        this.eventDetector = new ElevationDetector(MAX_CHECK, THRESHOLD, this.stationFrame).withConstantElevation(gsElevation).withHandler(this);
        // Clear visibility windows and visibility circles
        this.visibilityWindows.clear();
        this.currentVisibilityMap.clear();
        // Raise callback to notify parameter updates --> must trigger orbit recomputation
        if(notify) {
            notifyGroundStationUpdated();
        }
    }

    public synchronized TopocentricFrame getStationFrame() {
        if(this.stationFrame == null) {
            recomputeData(false);
        }
        return this.stationFrame;
    }

    // Temporary variables used for visibility computation
    private transient Map<Orbit, Date> temporaryPointMap = new HashMap<>();
    private transient Orbit currentOrbit;
    private transient boolean eventRaised;

    @Override
    public synchronized void init(SpacecraftState initialState, AbsoluteDate target, ElevationDetector detector) {
        // Nothing to do
    }

    @Override
    public synchronized Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
        this.eventRaised = true;
        Date eventTime = s.getDate().toDate(TimeScalesFactory.getUTC());
        if (increasing) {
            if(temporaryPointMap.containsKey(currentOrbit)) {
                Date previousTime = temporaryPointMap.remove(currentOrbit);
                VisibilityWindow vw = new VisibilityWindow(currentOrbit, currentOrbit.computeOrbitNumberAt(previousTime), null, previousTime, this);
                visibilityWindows.computeIfAbsent(currentOrbit, o -> new ArrayList<>()).add(vw);
            }
            temporaryPointMap.put(currentOrbit, eventTime);
            return Action.CONTINUE;
        } else {
            if(temporaryPointMap.containsKey(currentOrbit)) {
                Date previousTime = temporaryPointMap.remove(currentOrbit);
                VisibilityWindow vw = new VisibilityWindow(currentOrbit, currentOrbit.computeOrbitNumberAt(previousTime), previousTime, eventTime, this);
                visibilityWindows.computeIfAbsent(currentOrbit, o -> new ArrayList<>()).add(vw);
            } else {
                // End of pass but no start: pass in progress
                VisibilityWindow vw = new VisibilityWindow(currentOrbit, currentOrbit.computeOrbitNumberAt(eventTime), null, eventTime, this);
                visibilityWindows.computeIfAbsent(currentOrbit, o -> new ArrayList<>()).add(vw);
            }
            return Action.CONTINUE;
        }
    }

    @Override
    public synchronized SpacecraftState resetState(ElevationDetector detector, SpacecraftState oldState) {
        return oldState;
    }

    @Override
    public synchronized EventDetector getEventDetector() {
        if(this.eventDetector == null) {
            recomputeData(false);
        }
        return this.eventDetector;
    }

    @Override
    public synchronized void startVisibilityComputation(Orbit o) {
        // Nothing to do
        this.visibilityUpdateInProgress = true;
    }

    @Override
    public synchronized void initVisibilityComputation(Orbit orbit, Date time) {
        this.visibilityWindows.remove(orbit);
        this.temporaryPointMap.clear();
        this.currentOrbit = orbit;
        this.eventRaised = false;
    }

    @Override
    public synchronized void finalizeVisibilityComputation(Orbit orbit, SpacecraftPosition currentSpacecraftPosition) {
        Date previousTime = this.temporaryPointMap.remove(orbit);
        // Pass start but not complete: null end date
        if(previousTime != null) {
            VisibilityWindow vw = new VisibilityWindow(orbit, orbit.computeOrbitNumberAt(previousTime), previousTime, null, this);
            visibilityWindows.computeIfAbsent(orbit, o -> new ArrayList<>()).add(vw);
        }
        // Verify current visibility
        // As from https://www.orekit.org/mailing-list-archives/orekit-users/msg00625.html
        if(currentSpacecraftPosition != null) {
            SpacecraftState spacecraftState = currentSpacecraftPosition.getSpacecraftState();
            Date sTime = spacecraftState.getDate().toDate(TimeScalesFactory.getUTC());
            PVCoordinates pv = spacecraftState.getFrame().getTransformTo(this.stationFrame, spacecraftState.getDate()).transformPVCoordinates(spacecraftState.getPVCoordinates());
            Vector3D p = pv.getPosition();
            if (p.getDelta() > 0) {
                // Spacecraft is visible: update information
                int orbitNumber = currentSpacecraftPosition.getOrbitNumber();
                double[] azimuthElevation = getAzimuthElevationOf(spacecraftState);
                this.currentVisibilityMap.put(currentOrbit, new TrackPoint(sTime, currentSpacecraftPosition, this, azimuthElevation[0], azimuthElevation[1]));
                if (!eventRaised) {
                    // Create a fake visibility window
                    VisibilityWindow vw = new VisibilityWindow(currentOrbit, orbitNumber, null, null, this);
                    visibilityWindows.computeIfAbsent(currentOrbit, o -> new ArrayList<>()).add(0, vw);
                }
            } else {
                // Spacecraft is not visible: remove information
                this.currentVisibilityMap.remove(currentOrbit);
            }
            // Compute visibility circle (AOS at GS_ELEVATION in radians) using the current S/C height
            List<GeodeticPoint> visibilityCircle = new ArrayList<>(180);
            double gsElevation = Math.toRadians(configuration.getElevationThreshold());
            for (int i = 0; i < 180; ++i) {
                double azimuth = i * (2.0 * Math.PI / 180);
                visibilityCircle.add(getStationFrame().computeLimitVisibilityPoint(currentSpacecraftPosition.getLatLonHeight().getAltitude() + EarthReferenceUtils.REAL_EARTH_RADIUS_METERS, azimuth, gsElevation));
            }
            this.visibilityCircles.put(orbit, new VisibilityCircle(visibilityCircle));
            // Process finished, the endVisibilityComputation() method will be called by Orbit, and the listeners will be notified
            this.currentOrbit = null;
            this.temporaryPointMap.clear();
        }
    }

    @Override
    public synchronized void propagationModelAvailable(Orbit orbit, Date referenceDate, Propagator modelPropagator) {
        List<VisibilityWindow> windows = this.visibilityWindows.get(orbit);
        if(windows != null) {
            windows.forEach(o -> o.initialiseGroundTrack(orbit, modelPropagator));
        }
    }

    @Override
    public synchronized void endVisibilityComputation(Orbit orbit) {
        List<VisibilityWindow> windows = this.visibilityWindows.get(orbit);
        if(windows != null) {
            windows = List.copyOf(windows);
        }
        this.visibilityUpdateInProgress = false;
        notifyOrbitEffectListeners(orbit, windows, this.visibilityCircles.get(orbit), this.currentVisibilityMap.get(orbit));
    }

    public synchronized Map<Orbit, List<VisibilityWindow>> getAllVisibilityWindows() {
        return Map.copyOf(this.visibilityWindows);
    }

    public synchronized List<VisibilityWindow> getVisibilityWindowsOf(Orbit o) {
        return List.copyOf(this.visibilityWindows.get(o));
    }

    public synchronized Map<Orbit, TrackPoint> getAllCurrentVisibilities() {
        return Map.copyOf(this.currentVisibilityMap);
    }

    public synchronized TrackPoint getCurrentVisibilityOf(Orbit o) {
        return this.currentVisibilityMap.get(o);
    }

    public synchronized Map<Orbit, VisibilityCircle> getAllVisibilityCircles() {
        return Map.copyOf(this.visibilityCircles);
    }

    public synchronized VisibilityCircle getVisibilityCircleOf(Orbit o) {
        return this.visibilityCircles.get(o);
    }

    public synchronized double[] getAzimuthElevationOf(SpacecraftState ss) {
        PVCoordinates pv = ss.getFrame().getTransformTo(getStationFrame(), ss.getDate()).transformPVCoordinates(ss.getPVCoordinates());
        Vector3D p = pv.getPosition();
        double azimuth   = Math.toDegrees(p.getAlpha());
        if(azimuth < 0) {
            azimuth += 360.0;
        }
        double elevation = p.getDelta();
        // adjust azimuth: remove 90 degrees, then flip angle
        azimuth -= 90.0;
        azimuth = -azimuth;
        if(azimuth < 0) {
            azimuth += 360;
        }
        return new double[] { azimuth, Math.toDegrees(elevation) };
    }

    @Override
    public synchronized void orbitAdded(OrbitManager manager, Orbit orbit) {
        // Do nothing at this stage
    }

    @Override
    public synchronized void orbitRemoved(OrbitManager manager, Orbit orbit) {
        // Get rid of the related orbital data
        this.visibilityCircles.remove(orbit);
        this.temporaryPointMap.remove(orbit);
        this.currentVisibilityMap.remove(orbit);
        this.visibilityWindows.remove(orbit);
    }

    @Override
    public synchronized void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        // Do nothing, updates will be triggered
    }

    @Override
    public synchronized void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        TrackPoint tp = getTrackPointOf(currentPosition);
        if(tp == null && this.currentVisibilityMap.containsKey(orbit)) {
            this.currentVisibilityMap.remove(orbit);
            notifySpacecraftPositionListeners(orbit, null);
        } else if(tp != null) {
            this.currentVisibilityMap.put(orbit, tp);
            notifySpacecraftPositionListeners(orbit, tp);
        }
    }

    public synchronized TrackPoint getTrackPointOf(SpacecraftPosition sp) {
        double[] azEl = getAzimuthElevationOf(sp.getSpacecraftState());
        if(azEl[1] < 0) {
            return null;
        } else {
            return new TrackPoint(sp.getTime(), sp, this, azEl[0], azEl[1]);
        }
    }

    private void notifySpacecraftPositionListeners(Orbit orbit, TrackPoint currentPoint) {
        this.listeners.forEach(o -> {
            IGroundStationListener l = o.get();
            if(l != null) {
                l.spacecraftPositionUpdated(this, orbit, currentPoint);
            }
        });
    }

    private void notifyOrbitEffectListeners(Orbit orbit, List<VisibilityWindow> windows, VisibilityCircle visibilityCircle, TrackPoint currentPoint) {
        this.listeners.forEach(o -> {
            IGroundStationListener l = o.get();
            if(l != null) {
                l.groundStationOrbitDataUpdated(this, orbit, windows, visibilityCircle, currentPoint);
            }
        });
    }

    private void notifyGroundStationUpdated() {
        this.listeners.forEach(o -> {
            IGroundStationListener l = o.get();
            if(l != null) {
                l.groundStationUpdated(this);
            }
        });
    }

    public void exportVisibilityPasses(OutputStream outputStream, List<UUID> orbitsId) throws IOException {
        for(Map.Entry<Orbit, List<VisibilityWindow>> entry : visibilityWindows.entrySet()) {
            if(orbitsId == null || orbitsId.contains(entry.getKey().getId())) {
                for(VisibilityWindow vw : entry.getValue()) {
                    vw.exportVisibilityInfoTo(outputStream);
                }
            }
        }
    }

    public void exportTrackingInfo(OutputStream outputStream, UUID orbitId, UUID visibilityWindowId) throws IOException {
        for(Map.Entry<Orbit, List<VisibilityWindow>> entry : visibilityWindows.entrySet()) {
            if(orbitId.equals(entry.getKey().getId())) {
                for(VisibilityWindow vw : entry.getValue()) {
                    if(vw.getId().equals(visibilityWindowId)) {
                        vw.exportGroundTrackingInfoTo(outputStream);
                    }
                }
            }
        }
    }
}
