package eu.dariolucia.drorbiteex.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class CeletrakTleOrbit extends TleOrbit {

    private String group;

    @XmlAttribute
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String retrieveUpdatedTle() {
        // TODO: connect to https://celestrak.org/NORAD/elements/gp.php?GROUP=<group>&FORMAT=tle
        // TODO: parse the result and look for satellite name == name
        return null;
    }
}
