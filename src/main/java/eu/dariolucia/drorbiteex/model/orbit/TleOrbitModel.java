/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.drorbiteex.model.orbit;

import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;

@XmlAccessorType(XmlAccessType.PROPERTY)
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
    public synchronized String getTle() {
        return tle;
    }

    @XmlTransient
    public synchronized TLE getTleObject() {
        return this.tleObject;
    }

    private void setTle(String tle) {
        if(tle == null || tle.isBlank()) {
            throw new IllegalArgumentException("tle argument invalid: " + tle);
        }
        if(!tle.equals(this.tle)) {
            this.tle = tle;
            initialiseTle();
        }
    }

    @Override
    public synchronized Propagator getPropagator() {
        if(this.tlePropagator == null) {
            throw new IllegalStateException("tlePropagator cannot be null at this point");
        }
        return this.tlePropagator;
    }

    @Override
    public synchronized boolean updateModel(IOrbitModel model) {
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

    @Override
    public synchronized int computeOrbitNumberAt(Date time) {
        if(this.tleObject != null) {
            // Get the reference orbit
            Orbit o = this.tlePropagator.getInitialState().getOrbit();
            double period = o.getKeplerianPeriod();
            long orbitsAtEpoch = this.tleObject.getRevolutionNumberAtEpoch();
            AbsoluteDate epochTime = this.tleObject.getDate();
            // If you know how many orbits - orbitsAtEpoch - it had at epochTime, then you have to check the difference in
            // seconds between the epoch time and the time and, knowing the period, compute the orbit accordingly.
            long timeDifferenceSec = (time.getTime() - epochTime.toDate(TimeScalesFactory.getUTC()).getTime())/1000;
            long periodSec = Math.round(period);
            // Number of orbits in the time difference (integral number)
            // Obviously, if the time difference is negative, the number of orbits will be negatives
            long orbits = timeDifferenceSec/periodSec;
            return (int) (orbitsAtEpoch + orbits);
        } else {
            return -1;
        }
    }

    @Override
    public IOrbitModel copy() {
        return new TleOrbitModel(getTle());
    }
}
