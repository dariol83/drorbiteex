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

import eu.dariolucia.drorbiteex.model.util.EarthReferenceUtils;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.NodeDetector;
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
    private transient Date firstCrossTime; // Time of right ascension node crossing after epoch
    private transient int orbitAtCrossTime;

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
        // Get the initial position
        SpacecraftState initialSpacecraftState = this.tlePropagator.getInitialState();
        // Get the keplerian period
        Orbit o = initialSpacecraftState.getOrbit();
        double period = o.getKeplerianPeriod();
        long periodSec = Math.round(period);
        // Compute the time the spacecraft crosses the right ascension node (first time after epoch time)
        firstCrossTime = computeCrossRightAscensionNodeAfter(initialSpacecraftState, periodSec);
        orbitAtCrossTime = this.tleObject.getRevolutionNumberAtEpoch() + 1;
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
            // Get the keplerian period
            Orbit o = this.tlePropagator.getInitialState().getOrbit();
            double period = o.getKeplerianPeriod();
            // If you know how many orbits - orbitsAtFirstCross - it had at firstCrossTime, then you have to check the difference in
            // seconds between the firstCrossTime and the time and, knowing the period, compute the orbit accordingly.
            long timeDifferenceSec = (time.getTime() - firstCrossTime.getTime())/1000;
            // Number of orbits in the time difference (integral number)
            // Obviously, if the time difference is negative, the number of orbits will be negatives
            double orbits = timeDifferenceSec/period;
            return (int) Math.floor(orbitAtCrossTime + orbits);
        } else {
            return -1;
        }
    }

    private Date computeCrossRightAscensionNodeAfter(SpacecraftState initialSpacecraftState, long keplerianPeriodSec) {
        this.tlePropagator.resetInitialState(initialSpacecraftState);
        NodeDetector detector = new NodeDetector(0.001, initialSpacecraftState.getOrbit(), EarthReferenceUtils.getITRF());
        EventsLogger el = new EventsLogger();
        EventDetector ed = el.monitorDetector(detector);
        this.tlePropagator.addEventDetector(ed);
        this.tlePropagator.propagate(initialSpacecraftState.getDate().shiftedBy(2 * keplerianPeriodSec));
        Date found = null;
        if(!el.getLoggedEvents().isEmpty()) {
            // Get the latest event, it should be the right one
            for(int i = el.getLoggedEvents().size() - 1; i >= 0; i--) {
                if(el.getLoggedEvents().get(i).isIncreasing()) {
                    found = el.getLoggedEvents().get(i).getDate().toDate(TimeScalesFactory.getUTC());
                    break;
                }
            }
        }
        this.tlePropagator.clearEventsDetectors();
        this.tlePropagator.resetInitialState(initialSpacecraftState);
        return found;
    }

    @Override
    public IOrbitModel copy() {
        return new TleOrbitModel(getTle());
    }
}
