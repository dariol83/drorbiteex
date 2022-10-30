package eu.dariolucia.drorbiteex.model;

import eu.dariolucia.drorbiteex.data.Utils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import javax.xml.bind.annotation.*;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@XmlRootElement(name = "orbit", namespace = "http://dariolucia.eu/drorbiteex/orbit")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Orbit {

    private static final int PROPAGATION_STEPS = 100;
    private static final double PROPAGATION_STEP_DURATION = 60.0;
    private UUID id;
    private final SimpleStringProperty code = new SimpleStringProperty("");
    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty color = new SimpleStringProperty("0xFFFFFF");
    private final SimpleBooleanProperty visible = new SimpleBooleanProperty(false);
    private IOrbitModel model;

    private transient List<WeakReference<IOrbitListener>> listeners = new CopyOnWriteArrayList<>();
    private transient List<SpacecraftPosition> spacecraftPositions = new ArrayList<>();
    private transient Propagator modelPropagator = null;
    private transient Date currentPositionTime = new Date();
    private transient SpacecraftPosition currentSpacecraftPosition = null;

    public Orbit() {
        //
    }

    public Orbit(UUID id, String code, String name, String color, boolean visible, IOrbitModel model) {
        if(model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.id = id;
        this.code.set(code);
        this.name.set(name);
        this.color.set(color);
        this.visible.set(visible);
        this.model = model;

        forceDataUpdate();
    }

    @XmlAttribute(required = true)
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
    @XmlAttribute(required = true)
    public String getCode() {
        return code.get();
    }

    public SimpleStringProperty codeProperty() {
        return code;
    }

    public void setCode(String code) {
        this.code.set(code);
    }

    @XmlAttribute(required = true)
    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    @XmlAttribute
    public String getColor() {
        return color.get();
    }

    public SimpleStringProperty colorProperty() {
        return color;
    }

    public void setColor(String color) {
        this.color.set(color);
    }

    @XmlAttribute
    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public SimpleBooleanProperty visibleProperty() {
        return this.visible;
    }

    @XmlElement
    public IOrbitModel getModel() {
        return model;
    }

    public void setModel(IOrbitModel model) {
        if(model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
    }

    @Override
    public String toString() {
        return this.code + " - " + this.name;
    }

    public final void update(Orbit gs) {
        setCode(gs.getCode());
        setName(gs.getName());
        setColor(gs.getColor());
        setVisible(gs.isVisible());
        boolean modelUpdated = getModel().updateModel(gs.getModel());
        // If information about the orbit propagation model is updated, the model data must be recomputed
        if(modelUpdated) {
            recomputeData(this.currentPositionTime);
        }
    }

    private void recomputeData(Date referenceDate) {
        this.modelPropagator = model.getPropagator();
        this.spacecraftPositions.clear();
        AbsoluteDate ad = Utils.toAbsoluteDate(Objects.requireNonNullElse(referenceDate, new Date()));
        // Propagate in 3 steps
        // Past
        for (int i = -PROPAGATION_STEPS; i < 0; ++i) {
            // Propagate for 100 minutes (1 point every 1 minute - 100 points) // TODO: refactor, make propagation time configurable
            SpacecraftState next = this.modelPropagator.propagate(ad.shiftedBy(PROPAGATION_STEP_DURATION * i));
            this.spacecraftPositions.add(new SpacecraftPosition(this, next));
        }
        // Recompute current spacecraft position
        this.currentSpacecraftPosition = new SpacecraftPosition(this, getPropagator().propagate(ad));
        // Future, register event detectors from listeners
        List<IVisibilityDetector> detectors = this.listeners.stream().map(o -> {
            IOrbitListener l = o.get();
            if(l != null) {
                return l.getEventDetector();
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // Add detectors
        detectors.forEach(o -> {
            this.modelPropagator.addEventDetector(o);
            o.initVisibilityComputation(this, ad.toDate(TimeScalesFactory.getUTC()));
        });
        for (int i = 1; i < PROPAGATION_STEPS; ++i) {
            // Propagate for 100 minutes (1 point every 1 minute - 100 points) // TODO: refactor, make propagation time configurable
            SpacecraftState next = this.modelPropagator.propagate(ad.shiftedBy(PROPAGATION_STEP_DURATION * i));
            this.spacecraftPositions.add(new SpacecraftPosition(this, next));
        }
        // Declare end for detectors, clear detectors
        detectors.forEach(o -> {
            o.endVisibilityComputation(this);
        });
        this.modelPropagator.clearEventsDetectors();
        // Notify listeners
        notifyDataUpdate();
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

    public void clearListeners(IOrbitListener l) {
        this.listeners.clear();
    }

    public SpacecraftState updateOrbitTime(Date time) {
        Date previousTime = this.currentPositionTime;
        this.currentPositionTime = time;
        if(Duration.between(previousTime.toInstant(), time.toInstant()).getSeconds() > 60 * 30) {
            recomputeData(this.currentPositionTime);
        } else {
            // Compute only the position of the spacecraft, notify listeners about new spacecraft position
            this.currentSpacecraftPosition = new SpacecraftPosition(this, getPropagator().propagate(Utils.toAbsoluteDate(time)));
        }
        // Notify
        notifySpacecraftPositionUpdate();
        // Return value
        return this.currentSpacecraftPosition.getSpacecraftState();
    }

    public void forceDataUpdate() {
        recomputeData(this.currentPositionTime);
    }

    public Propagator getPropagator() {
        return this.modelPropagator;
    }
}
