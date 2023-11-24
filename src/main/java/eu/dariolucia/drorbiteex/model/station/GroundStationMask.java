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
        // Build dense, min 1deg AZ list, with 0 and 360
        List<MaskEntry> entryList = new LinkedList<>();
        MaskEntry previousEntry = null;
        // Add 0 degree: take the last entry in the original list
        if(getEntries().get(0).getAzimuth() > 0) {
            MaskEntry zeroDegEntry = new MaskEntry(0, getEntries().get(getEntries().size() - 1).getElevation());
            entryList.add(zeroDegEntry);
            previousEntry = zeroDegEntry;
        }
        // Check if you have to add anything from 0 deg to the first entry
        if(previousEntry != null) {
            double difference = getEntries().get(0).getAzimuth() - previousEntry.getAzimuth();
            double startingAzimuth = previousEntry.getAzimuth();
            while(difference > 1.0) {
                startingAzimuth += 1.0;
                previousEntry = new MaskEntry(startingAzimuth, previousEntry.getElevation());
                entryList.add(previousEntry);
                difference -= 1.0;
            }
        }
        // Now add the data you have
        for(MaskEntry e: getEntries()) {
            if(previousEntry != null && e.getAzimuth() - previousEntry.getAzimuth() > 1.0) {
                // Add intermediates
                double difference = e.getAzimuth() - previousEntry.getAzimuth();
                double startingAzimuth = previousEntry.getAzimuth();
                while(difference > 1.0) {
                    startingAzimuth += 1.0;
                    entryList.add(new MaskEntry(startingAzimuth, previousEntry.getElevation()));
                    difference -= 1.0;
                }
            }
            // Add current
            entryList.add(e);
            previousEntry = e;
        }
        // Check if you have to add anything from previous entry to 360 deg
        if(previousEntry != null) {
            double difference = 360 - previousEntry.getAzimuth();
            double startingAzimuth = previousEntry.getAzimuth();
            while(difference > 0.0) {
                startingAzimuth += 1.0;
                if(startingAzimuth > 360.0) {
                    startingAzimuth = 360.0;
                }
                previousEntry = new MaskEntry(startingAzimuth, previousEntry.getElevation());
                entryList.add(previousEntry);
                difference -= 1.0;
            }
        }
        // Construct array
        azElMap[0] = new double[entryList.size()];
        azElMap[1] = new double[entryList.size()];
        for(int i = 0; i < entryList.size(); ++i) {
            MaskEntry e = entryList.get(i);
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
