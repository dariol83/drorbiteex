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

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import org.orekit.bodies.GeodeticPoint;

import java.util.List;

public interface IGroundStationListener {
    void groundStationAdded(GroundStationManager manager, GroundStation groundStation);

    void groundStationRemoved(GroundStationManager manager, GroundStation groundStation);

    void groundStationUpdated(GroundStation groundStation);

    void groundStationOrbitDataUpdated(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows, VisibilityCircle visibilityCircle, TrackPoint currentPoint);

    void spacecraftPositionUpdated(GroundStation groundStation, Orbit orbit, TrackPoint point);

}
