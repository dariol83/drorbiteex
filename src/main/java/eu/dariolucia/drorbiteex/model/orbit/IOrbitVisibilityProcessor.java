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

import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.EventDetector;

import java.util.Date;

public interface IOrbitVisibilityProcessor extends IOrbitListener {

    EventDetector getEventDetector();

    void startVisibilityComputation(Orbit o);

    void initVisibilityComputation(Orbit o, Date startTime);

    void finalizeVisibilityComputation(Orbit o, SpacecraftPosition currentSpacecraftPosition);

    void propagationModelAvailable(Orbit orbit, Date referenceDate, Propagator modelPropagator);

    void endVisibilityComputation(Orbit o);
}
