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
import javax.xml.bind.annotation.XmlElement;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStationMask {

    private List<MaskEntry> entries = new LinkedList<>();

    private transient volatile double[][] azElMap = null;

    @XmlElement(name = "entry")
    public List<MaskEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<MaskEntry> entries) {
        this.entries = entries;
        updateAzElMap();
    }

    private synchronized void updateAzElMap() {
        azElMap = new double[2][];
        azElMap[0] = new double[entries.size()];
        azElMap[1] = new double[entries.size()];
        for(int i = 0; i < entries.size(); ++i) {
            MaskEntry e = entries.get(i);
            azElMap[0][i] = e.getAzimuth();
            azElMap[1][i] = e.getElevation();
        }
    }

    public synchronized double[][] getAzElMap() {
        if(azElMap == null) {
            updateAzElMap();
        }
        return azElMap;
    }

    public GroundStationMask copy() {
        GroundStationMask mask = new GroundStationMask();
        for(MaskEntry me : getEntries()) {
            mask.getEntries().add(me.copy());
        }
        return mask;
    }
}
