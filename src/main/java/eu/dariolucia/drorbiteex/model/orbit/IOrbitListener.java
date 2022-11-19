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

package eu.dariolucia.drorbiteex.model.orbit;

import java.util.Date;
import java.util.List;

public interface IOrbitListener {

    void orbitAdded(OrbitManager manager, Orbit orbit);

    void orbitRemoved(OrbitManager manager, Orbit orbit);

    void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition);

    void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition);

    default void startOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        // Do nothing
    }

    default void endOrbitTimeUpdate(Date referenceTime, boolean isForced) {
        // Do nothing
    }
}
