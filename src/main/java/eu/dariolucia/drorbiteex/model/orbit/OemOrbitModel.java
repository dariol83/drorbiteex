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

import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class OemOrbitModel implements IOrbitModel {

    private String oem;
    private Oem oemObject;
    private BoundedPropagator oemPropagator;

    public OemOrbitModel() {
        //
    }

    public OemOrbitModel(String oem) {
        if(oem == null || oem.isBlank()) {
            throw new IllegalArgumentException("oem argument invalid: " + oem);
        }
        this.oem = oem;
        initialiseOem();
    }

    private void initialiseOem() {
        OemParser parser = new ParserBuilder().buildOemParser();
        this.oemObject = parser.parse(new DataSource("oem", () -> new ByteArrayInputStream(oem.getBytes(StandardCharsets.UTF_8))));
        // Well... next call can deserve surprises, perhaps it would be better to handle it differently
        this.oemPropagator = this.oemObject.getSatellites().values().iterator().next().getPropagator();
    }

    @XmlElement
    @XmlJavaTypeAdapter(Base64Adapter.class)
    public synchronized String getOem() {
        return oem;
    }

    private void setOem(String oem) {
        if(oem == null || oem.isBlank()) {
            throw new IllegalArgumentException("oem argument invalid: " + oem);
        }
        if(!oem.equals(this.oem)) {
            this.oem = oem;
            initialiseOem();
        }
    }

    @Override
    public synchronized Propagator getPropagator() {
        if(this.oemPropagator == null) {
            throw new IllegalStateException("oemPropagator cannot be null at this point");
        }
        return this.oemPropagator;
    }

    @Override
    public synchronized boolean updateModel(IOrbitModel model) {
        if(model instanceof OemOrbitModel) {
            OemOrbitModel iModel = (OemOrbitModel) model;
            if(!iModel.getOem().equals(this.oem)) {
                this.oem = iModel.getOem();
                initialiseOem();
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
        if(this.oemObject != null) {
            // Get the reference orbit
            Orbit o = this.oemPropagator.getInitialState().getOrbit();
            double period = o.getKeplerianPeriod();
            long orbitsAtEpoch = 0; // the OEM has no orbit number information
            AbsoluteDate epochTime = this.oemPropagator.getMinDate();
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
        return new OemOrbitModel(getOem());
    }

    public static class Base64Adapter extends XmlAdapter<byte[], String> {
        @Override
        public byte[] marshal(String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String unmarshal(byte[] s) {
            return new String(s, StandardCharsets.UTF_8);
        }
    }
}
