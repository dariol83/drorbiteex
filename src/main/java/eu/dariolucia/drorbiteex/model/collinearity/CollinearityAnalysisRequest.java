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

package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class CollinearityAnalysisRequest extends AbstractTimeIntervalAnalysisRequest {

    private final GroundStation groundStation;
    private final Orbit referenceOrbit;
    private final double minAngularSeparation;
    private final int intervalPeriod;
    private final int cores;
    private final Set<String> orbitExclusions;

    private final int minHeight;
    private final int maxHeight;
    private final String celestrakGroup;
    private final List<Orbit> targetOrbits;

    public CollinearityAnalysisRequest(Date startTime, Date endTime, GroundStation groundStation, Orbit referenceOrbit,
                                       double minAngularSeparation, int intervalPeriod, int cores,
                                       Collection<String> orbitExclusions, Integer minHeight, Integer maxHeight,
                                       String celestrakGroup,
                                       List<Orbit> targetOrbits) {
        super(startTime, endTime);
        // Copy the ground station and the reference orbit
        this.groundStation = groundStation.copy();
        this.referenceOrbit = referenceOrbit.copy();
        this.minAngularSeparation = minAngularSeparation;
        this.intervalPeriod = intervalPeriod;
        this.cores = cores;
        this.orbitExclusions = Set.copyOf(orbitExclusions);
        this.minHeight = minHeight != null ? minHeight : 0;
        this.maxHeight = maxHeight != null ? maxHeight : Integer.MAX_VALUE;
        this.celestrakGroup = celestrakGroup;
        this.targetOrbits = targetOrbits;
    }

    public List<Orbit> getTargetOrbits() {
        return targetOrbits;
    }

    public String getCelestrakGroup() {
        return celestrakGroup;
    }

    public Set<String> getOrbitExclusions() {
        return orbitExclusions;
    }

    public GroundStation getGroundStation() {
        return groundStation;
    }

    public Orbit getReferenceOrbit() {
        return referenceOrbit;
    }

    public double getMinAngularSeparation() {
        return minAngularSeparation;
    }

    public int getCores() {
        return cores;
    }

    public int getIntervalPeriod() {
        return intervalPeriod;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMinHeight() {
        return minHeight;
    }

}
