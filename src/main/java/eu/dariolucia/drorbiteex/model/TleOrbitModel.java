package eu.dariolucia.drorbiteex.model;

import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;

import javax.xml.bind.annotation.XmlElement;


public class TleOrbitModel implements IOrbitModel {

    private String tle;

    private transient TLEPropagator tlePropagator;
    private transient TLE tleObject;

    public TleOrbitModel() {
        //
    }

    public TleOrbitModel(String tle) {
        if(tle == null || tle.isBlank()) {
            throw new IllegalArgumentException("tle argument invalid: " + tle);
        }
        this.tle = tle;
        initialiseTle();
    }

    private void initialiseTle() {
        this.tleObject = new TLE(this.tle.substring(0, this.tle.indexOf("\n")).trim(), this.tle.substring(this.tle.indexOf("\n")).trim());
        this.tlePropagator = TLEPropagator.selectExtrapolator(tleObject);
    }

    @XmlElement
    public String getTle() {
        return tle;
    }

    public void setTle(String tle) {
        if(tle == null || tle.isBlank()) {
            throw new IllegalArgumentException("tle argument invalid: " + tle);
        }
        if(!tle.equals(this.tle)) {
            this.tle = tle;
            initialiseTle();
        }
    }

    @Override
    public Propagator getPropagator() {
        return this.tlePropagator;
    }

    @Override
    public boolean updateModel(IOrbitModel model) {
        if(model instanceof TleOrbitModel) {
            TleOrbitModel iModel = (TleOrbitModel) model;
            if(!iModel.getTle().equals(this.tle)) {
                this.tle = iModel.getTle();
                initialiseTle();
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Not a class of type TleOrbitModel");
        }
    }
}
