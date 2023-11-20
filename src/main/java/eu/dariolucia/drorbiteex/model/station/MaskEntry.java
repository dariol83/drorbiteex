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

package eu.dariolucia.drorbiteex.model.station;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class MaskEntry implements Comparable<MaskEntry> {

    private double azimuth;
    private double elevation;

    public MaskEntry(double azimuth, double elevation) {
        this.azimuth = azimuth;
        this.elevation = elevation;
    }

    public MaskEntry() {
    }

    @XmlAttribute
    public double getAzimuth() {
        return azimuth;
    }

    private void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    @XmlAttribute
    public double getElevation() {
        return elevation;
    }

    private void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public MaskEntry copy() {
        return new MaskEntry(azimuth, elevation);
    }

    @Override
    public int compareTo(MaskEntry o) {
        if(getAzimuth() == o.getAzimuth()) {
            return Double.compare(getElevation(), o.getElevation());
        } else {
            return Double.compare(getAzimuth(), o.getAzimuth());
        }
    }

    @Override
    public String toString() {
        return String.format("%.3f -> %.3f", getAzimuth(), getElevation());
    }
}
