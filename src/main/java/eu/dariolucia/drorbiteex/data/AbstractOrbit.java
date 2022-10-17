package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class AbstractOrbit {

    private String code;
    private String name;
    private String color;
    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);

    private transient Group groupItem;

    @XmlAttribute
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @XmlAttribute
    public boolean isVisible() {
        return visibleProperty.get();
    }

    public void setVisible(boolean visible) {
        this.visibleProperty.set(visible);
    }

    public SimpleBooleanProperty visibleProperty() {
        return this.visibleProperty;
    }

    @Override
    public String toString() {
        return this.code + " - " + this.name;
    }

    public final Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        List<Node> toAdd = constructGraphicItems();
        updateGraphicItems();
        this.groupItem = new Group(toAdd);
        this.groupItem.visibleProperty().bind(this.visibleProperty);
        return this.groupItem;
    }

    protected abstract List<Node> constructGraphicItems();

    public final Group getGraphicItem() {
        return this.groupItem;
    }

    public final void dispose() {
        this.groupItem.visibleProperty().unbind();
        disposeGraphicItems();
        this.groupItem = null;
    }

    protected abstract void disposeGraphicItems();

    public final void update(AbstractOrbit gs) {
        System.out.println("Updating orbit of " + getName());
        this.code = gs.getCode();
        this.name = gs.getName();
        this.color = gs.getColor();

        updateProperties(gs);

        if(this.groupItem != null) {
            updateGraphicItems();
        }
    }

    protected abstract void updateGraphicItems();

    protected abstract void updateProperties(AbstractOrbit gs);

    public abstract void updateOrbitTime(Date time);
}
