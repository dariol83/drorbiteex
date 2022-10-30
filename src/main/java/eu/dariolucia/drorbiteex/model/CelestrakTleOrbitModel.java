package eu.dariolucia.drorbiteex.model;

import javax.xml.bind.annotation.XmlAttribute;


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
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
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
}
