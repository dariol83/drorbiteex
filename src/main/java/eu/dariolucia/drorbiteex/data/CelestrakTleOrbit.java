package eu.dariolucia.drorbiteex.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class CelestrakTleOrbit extends TleOrbit {

    private String group;

    @XmlAttribute
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
