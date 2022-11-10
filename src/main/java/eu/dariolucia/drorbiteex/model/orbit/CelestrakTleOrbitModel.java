package eu.dariolucia.drorbiteex.model.orbit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class CelestrakTleOrbitModel extends TleOrbitModel {

    private String group;

    public CelestrakTleOrbitModel() {
        //
    }

    public CelestrakTleOrbitModel(String group, String tle) {
        super(tle);
        this.group = group;
    }

    @XmlAttribute
    public synchronized String getGroup() {
        return group;
    }

    private void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean updateModel(IOrbitModel model) {
        if(model instanceof CelestrakTleOrbitModel) {
            CelestrakTleOrbitModel iModel = (CelestrakTleOrbitModel) model;
            this.group = iModel.getGroup();
            return super.updateModel(model);
        } else {
            throw new IllegalArgumentException("Not a class of type CelestrakTleOrbitModel");
        }
    }

    @Override
    public IOrbitModel copy() {
        return new CelestrakTleOrbitModel(getGroup(), getTle());
    }
}
