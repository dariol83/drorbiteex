/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

import org.orekit.bodies.GeodeticPoint;

import java.util.List;

public class VisibilityCircle {

    private final List<GeodeticPoint> visibilityCircle;
    private final boolean polarCircle;

    public VisibilityCircle(List<GeodeticPoint> visibilityCircle) {
        this.visibilityCircle = List.copyOf(visibilityCircle);
        this.polarCircle = isPolarVisibilityCircle(this.visibilityCircle);
    }

    public List<GeodeticPoint> getVisibilityCircle() {
        return visibilityCircle;
    }

    public boolean isPolarCircle() {
        return polarCircle;
    }

    private boolean isPolarVisibilityCircle(List<GeodeticPoint> visibilityCircle) {
        for(int i = 0; i < visibilityCircle.size(); ++i) {
            if(i < visibilityCircle.size() - 1) {
                if(Math.abs(visibilityCircle.get(i).getLongitude() - visibilityCircle.get(i + 1).getLongitude()) > Math.PI + 0.1) {
                    return true;
                }
            } else {
                if(Math.abs(visibilityCircle.get(i).getLongitude() - visibilityCircle.get(0).getLongitude()) > Math.PI + 0.1) {
                    return true;
                }
            }
        }
        return false;
    }
}
