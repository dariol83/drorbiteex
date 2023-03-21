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

import eu.dariolucia.drorbiteex.model.collinearity.ErrorPoint;
import org.orekit.propagation.Propagator;

import java.util.List;

public class OrbitDeterminationResult {

    private final OrbitDeterminationRequest request;
    private final Propagator estimatedPropagator;
    private final String estimatedTle;
    private final List<ErrorPoint> residuals;

    public OrbitDeterminationResult(OrbitDeterminationRequest request, Propagator estimatedPropagator, String estimatedTle, List<ErrorPoint> residuals) {
        this.request = request;
        this.estimatedPropagator = estimatedPropagator;
        this.estimatedTle = estimatedTle;
        this.residuals = residuals;
    }

    public OrbitDeterminationRequest getRequest() {
        return request;
    }

    public Propagator getEstimatedPropagator() {
        return estimatedPropagator;
    }

    public String getEstimatedTle() {
        return estimatedTle;
    }

    public List<ErrorPoint> getResiduals() {
        return residuals;
    }
}
