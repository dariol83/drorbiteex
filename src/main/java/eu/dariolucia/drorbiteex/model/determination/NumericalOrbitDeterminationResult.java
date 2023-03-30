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
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;

import java.util.List;

public class NumericalOrbitDeterminationResult {

    private final NumericalOrbitDeterminationRequest request;
    private final Propagator estimatedPropagator;
    private final Orbit orekitOrbit;
    private final List<ErrorPoint> residuals;

    public NumericalOrbitDeterminationResult(NumericalOrbitDeterminationRequest request, Propagator estimatedPropagator, Orbit orekitOrbit, List<ErrorPoint> residuals) {
        this.request = request;
        this.estimatedPropagator = estimatedPropagator;
        this.orekitOrbit = orekitOrbit;
        this.residuals = residuals;
    }

    public NumericalOrbitDeterminationRequest getRequest() {
        return request;
    }

    public Propagator getEstimatedPropagator() {
        return estimatedPropagator;
    }

    public Orbit getOrekitOrbit() {
        return orekitOrbit;
    }

    public List<ErrorPoint> getResiduals() {
        return residuals;
    }
}
