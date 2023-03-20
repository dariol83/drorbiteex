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

public class OrbitDeterminationRequest {

    private final Orbit orbit;

    private final double mass;
    private final Double crossSection;
    private final Double cr;
    private final Double cd;

    private final boolean useMoon;
    private final boolean useSun;
    private final boolean useRelativity;
    private final boolean useSolarPressure;
    private final boolean useAtmosphericDrag;

    private final List<Measurement> measurementList;

    public OrbitDeterminationRequest(Orbit orbit, double mass, Double crossSection, Double cr, Double cd, boolean useMoon, boolean useSun, boolean useRelativity, boolean useSolarPressure, boolean useAtmosphericDrag, List<Measurement> measurementList) {
        this.orbit = orbit;
        this.mass = mass;
        this.crossSection = crossSection;
        this.cr = cr;
        this.cd = cd;
        this.useMoon = useMoon;
        this.useSun = useSun;
        this.useRelativity = useRelativity;
        this.useSolarPressure = useSolarPressure;
        this.useAtmosphericDrag = useAtmosphericDrag;
        this.measurementList = measurementList;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public double getMass() {
        return mass;
    }

    public Double getCrossSection() {
        return crossSection;
    }

    public Double getCr() {
        return cr;
    }

    public Double getCd() {
        return cd;
    }

    public boolean isUseMoon() {
        return useMoon;
    }

    public boolean isUseSun() {
        return useSun;
    }

    public boolean isUseRelativity() {
        return useRelativity;
    }

    public boolean isUseSolarPressure() {
        return useSolarPressure;
    }

    public boolean isUseAtmosphericDrag() {
        return useAtmosphericDrag;
    }

    public List<Measurement> getMeasurementList() {
        return measurementList;
    }
}
