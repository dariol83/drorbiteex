package eu.dariolucia.drorbiteex.model.orbit;

import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import javax.xml.bind.annotation.*;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Orbit {

    private static final int PROPAGATION_STEPS = 100;
    private static final double PROPAGATION_STEP_DURATION = 60.0;

    // Subject to serialisation
    private volatile UUID id;
    private volatile String code = "";
    private volatile String name = "";
    private volatile String color = "0xFFFFFF";
    private volatile boolean visible = false;
    private volatile IOrbitModel model;

    // Transient state objects
    private transient volatile List<WeakReference<IOrbitListener>> listeners = new CopyOnWriteArrayList<>();
    private transient volatile List<SpacecraftPosition> spacecraftPositions = new ArrayList<>();
    private transient volatile Propagator modelPropagator = null;
    private transient volatile Date currentPositionTime = new Date();

    private transient volatile Date lastOrbitUpdateTime = null;
    private transient volatile SpacecraftPosition currentSpacecraftPosition = null;

    private Orbit() {
        //
    }

    public Orbit(UUID id, String code, String name, String color, boolean visible, IOrbitModel model) {
        if(model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.id = id;
        this.code = code;
        this.name = name;
        this.color = color;
        this.visible = visible;
        this.model = model;
    }

    @XmlAttribute(required = true)
    public synchronized UUID getId() {
        return id;
    }

    private synchronized void setId(UUID id) {
        this.id = id;
    }

    @XmlAttribute(required = true)
    public synchronized String getCode() {
        return code;
    }

    public synchronized void setCode(String code) {
        this.code = code;
        notifyDataUpdate();
    }

    @XmlAttribute(required = true)
    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
        notifyDataUpdate();
    }

    @XmlAttribute
    public synchronized String getColor() {
        return color;
    }

    public synchronized void setColor(String color) {
        this.color = color;
        notifyDataUpdate();
    }

    @XmlAttribute
    public synchronized boolean isVisible() {
        return visible;
    }

    public synchronized void setVisible(boolean visible) {
        this.visible = visible;
        notifyDataUpdate();
    }

    @XmlElements({
            @XmlElement(name="tle-model",type=TleOrbitModel.class),
            @XmlElement(name="tle-celestrak-model",type=CelestrakTleOrbitModel.class),
            @XmlElement(name="oem-model",type=OemOrbitModel.class)
    })
    public synchronized IOrbitModel getModel() {
        return model;
    }

    public synchronized void setModel(IOrbitModel model) {
        if(model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
    }

    @Override
    public synchronized String toString() {
        return this.code + " - " + this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orbit orbit = (Orbit) o;
        return Objects.equals(id, orbit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public final void update(Orbit gs) {
        this.code = gs.getCode();
        this.name = gs.getName();
        this.color = gs.getColor();
        this.visible = gs.isVisible();
        boolean modelUpdated = getModel().updateModel(gs.getModel());
        // If information about the orbit propagation model is updated, the model data must be recomputed
        if(modelUpdated) {
            recomputeData(this.currentPositionTime);
        } else {
            notifyDataUpdate();
        }
    }

    /**
     * This method performs a full recomputation of the orbital parameters, including callbacks and listener-related
     * processes.
     *
     * The referenceDate argument drives the propagation start time and end time, and it is used as current date to
     * initialise the current spacecraft position.
     *
     * @param referenceDate the reference date to use
     */
    private void recomputeData(Date referenceDate) {
        // Protect the call from null argument
        if(referenceDate == null) {
            referenceDate = new Date();
        }
        this.lastOrbitUpdateTime = referenceDate;
        this.modelPropagator = this.model.getPropagator();
        this.spacecraftPositions.clear();
        AbsoluteDate ad = TimeUtils.toAbsoluteDate(referenceDate);
        // Propagate in 3 steps
        // Past
        for (int i = -PROPAGATION_STEPS; i < 0; ++i) {
            // Propagate for 100 minutes (1 point every 1 minute - 100 points) // TODO: refactor, make propagation time configurable
            AbsoluteDate newDate = ad.shiftedBy(PROPAGATION_STEP_DURATION * i);
            int orbitNumber = computeOrbitNumberAt(newDate.toDate(TimeScalesFactory.getUTC()));
            SpacecraftState next = this.modelPropagator.propagate(newDate);
            this.spacecraftPositions.add(new SpacecraftPosition(this, orbitNumber, next));
        }
        // Recompute current spacecraft position
        int orbitNumber = computeOrbitNumberAt(ad.toDate(TimeScalesFactory.getUTC()));
        this.currentSpacecraftPosition = new SpacecraftPosition(this, orbitNumber, this.modelPropagator.propagate(ad));
        // Future, register event detectors from listeners
        List<IOrbitVisibilityProcessor> detectors = this.listeners.stream().map(o -> {
            IOrbitListener l = o.get();
            if(l instanceof IOrbitVisibilityProcessor) {
                return (IOrbitVisibilityProcessor) l;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // Add detectors
        detectors.forEach(o -> {
            EventDetector detector = o.getEventDetector();
            this.modelPropagator.addEventDetector(detector);
            o.initVisibilityComputation(this, ad.toDate(TimeScalesFactory.getUTC()));
        });
        for (int i = 1; i < PROPAGATION_STEPS; ++i) {
            // Propagate for 100 minutes (1 point every 1 minute - 100 points) // TODO: refactor, make propagation time configurable
            AbsoluteDate newDate = ad.shiftedBy(PROPAGATION_STEP_DURATION * i);
            orbitNumber = computeOrbitNumberAt(newDate.toDate(TimeScalesFactory.getUTC()));
            SpacecraftState next = this.modelPropagator.propagate(newDate);
            this.spacecraftPositions.add(new SpacecraftPosition(this, orbitNumber, next));
        }
        // Declare end for detectors, clear detectors
        detectors.forEach(o -> o.finalizeVisibilityComputation(this, this.currentSpacecraftPosition));
        this.modelPropagator.clearEventsDetectors();
        // Now: for every listener, move back the model propagation to the current date and offer the propagator to
        // each listener for visibility use (GroundStation) or other use.

        // Reset the propagator after every use
        for(IOrbitVisibilityProcessor vd : detectors) {
            this.modelPropagator.propagate(ad);
            vd.propagationModelAvailable(this, referenceDate, this.modelPropagator);
        }
        detectors.forEach(o -> o.endVisibilityComputation(this));

        // Notify listeners
        notifyDataUpdate();
    }

    public void propagate(Date start, Date end) {
        AbsoluteDate startDate = TimeUtils.toAbsoluteDate(start);
        AbsoluteDate endDate = TimeUtils.toAbsoluteDate(end);
        this.modelPropagator.propagate(startDate);
        // Future, register event detectors from listeners
        List<IOrbitVisibilityProcessor> detectors = this.listeners.stream().map(o -> {
            IOrbitListener l = o.get();
            if(l instanceof IOrbitVisibilityProcessor) {
                return (IOrbitVisibilityProcessor) l;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // Add detectors
        detectors.forEach(o -> {
            EventDetector detector = o.getEventDetector();
            this.modelPropagator.addEventDetector(detector);
            o.initVisibilityComputation(this, startDate.toDate(TimeScalesFactory.getUTC()));
        });
        // Propagate to end date
        this.modelPropagator.propagate(endDate);
        // Declare end for detectors, clear detectors
        detectors.forEach(o -> o.finalizeVisibilityComputation(this, this.currentSpacecraftPosition));
        this.modelPropagator.clearEventsDetectors();
        // Now: for every listener, move back the model propagation to the current date and offer the propagator to
        // each listener for visibility use (GroundStation) or other use.

        // Reset the propagator after every use
        for(IOrbitVisibilityProcessor vd : detectors) {
            this.modelPropagator.propagate(startDate);
            vd.propagationModelAvailable(this, start, this.modelPropagator);
        }
    }

    private void notifyDataUpdate() {
        // Notify listeners
        this.listeners.forEach(o -> {
            IOrbitListener l = o.get();
            if(l != null) {
                l.orbitModelDataUpdated(this, this.spacecraftPositions, this.currentSpacecraftPosition);
            }
        });
        this.listeners.removeIf(o -> o.get() == null);
    }

    private void notifySpacecraftPositionUpdate() {
        // Notify listeners
        this.listeners.forEach(o -> {
            IOrbitListener l = o.get();
            if(l != null) {
                l.spacecraftPositionUpdated(this, this.currentSpacecraftPosition);
            }
        });
    }

    public void addListener(IOrbitListener l) {
        this.listeners.add(new WeakReference<>(l));
    }

    public void removeListener(IOrbitListener l) {
        this.listeners.removeIf(o -> {
            IOrbitListener obj = o.get();
            return obj == null || obj == l;
        });
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    public synchronized SpacecraftState updateOrbitTime(Date time, boolean forceUpdate) {
        this.currentPositionTime = time;
        if(forceUpdate || this.modelPropagator == null || this.lastOrbitUpdateTime == null || Duration.between(this.lastOrbitUpdateTime.toInstant(), time.toInstant()).getSeconds() > 60 * 30) {
            recomputeData(this.currentPositionTime);
        } else {
            // Compute only the position of the spacecraft, notify listeners about new spacecraft position
            int orbitNumber = computeOrbitNumberAt(time);
            this.currentSpacecraftPosition = new SpacecraftPosition(this, orbitNumber, this.modelPropagator.propagate(TimeUtils.toAbsoluteDate(time)));
        }
        // Notify
        notifySpacecraftPositionUpdate();
        // Return value
        return this.currentSpacecraftPosition.getSpacecraftState();
    }

    public synchronized void refresh() {
        recomputeData(this.currentPositionTime);
    }

    public synchronized int computeOrbitNumberAt(Date time) {
        return this.model.computeOrbitNumberAt(time);
    }

    public synchronized List<SpacecraftPosition> getSpacecraftPositions() {
        return List.copyOf(spacecraftPositions);
    }

    public synchronized SpacecraftPosition getCurrentSpacecraftPosition() {
        return currentSpacecraftPosition;
    }
}
