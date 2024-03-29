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

package eu.dariolucia.drorbiteex.model.determination;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;

import java.util.List;

public class TleOrbitDeterminationRequest {

    private final Orbit orbit;
    private final double mass;
    private final List<Measurement> measurementList;

    public TleOrbitDeterminationRequest(Orbit orbit, double mass, List<Measurement> measurementList) {
        this.orbit = orbit;
        this.mass = mass;
        this.measurementList = measurementList;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public double getMass() {
        return mass;
    }

    public List<Measurement> getMeasurementList() {
        return measurementList;
    }
}
