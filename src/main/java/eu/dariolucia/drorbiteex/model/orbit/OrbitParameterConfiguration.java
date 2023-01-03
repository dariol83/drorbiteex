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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class OrbitParameterConfiguration {

    private int beforePropagationSteps = 50;
    private int afterPropagationSteps = 150;
    private int stepInterval = 120; // Seconds
    private int recomputeFullDataInterval = 600; // Seconds

    public OrbitParameterConfiguration() {
    }

    public OrbitParameterConfiguration(int beforePropagationSteps, int afterPropagationSteps, int stepInterval, int recomputeFullDataInterval) {
        this.beforePropagationSteps = beforePropagationSteps;
        this.afterPropagationSteps = afterPropagationSteps;
        this.stepInterval = stepInterval;
        this.recomputeFullDataInterval = recomputeFullDataInterval;
    }

    public void update(OrbitParameterConfiguration p) {
        this.beforePropagationSteps = p.beforePropagationSteps;
        this.afterPropagationSteps = p.afterPropagationSteps;
        this.stepInterval = p.stepInterval;
        this.recomputeFullDataInterval = p.recomputeFullDataInterval;
    }

    public int getBeforePropagationSteps() {
        return beforePropagationSteps;
    }

    public void setBeforePropagationSteps(int beforePropagationSteps) {
        this.beforePropagationSteps = beforePropagationSteps;
    }

    public int getAfterPropagationSteps() {
        return afterPropagationSteps;
    }

    public void setAfterPropagationSteps(int afterPropagationSteps) {
        this.afterPropagationSteps = afterPropagationSteps;
    }

    public int getStepInterval() {
        return stepInterval;
    }

    public void setStepInterval(int stepInterval) {
        this.stepInterval = stepInterval;
    }

    public int getRecomputeFullDataInterval() {
        return recomputeFullDataInterval;
    }

    public void setRecomputeFullDataInterval(int recomputeFullDataInterval) {
        this.recomputeFullDataInterval = recomputeFullDataInterval;
    }

    @Override
    public String toString() {
        return "OrbitParameterConfiguration{" +
                "beforePropagationSteps=" + beforePropagationSteps +
                ", afterPropagationSteps=" + afterPropagationSteps +
                ", stepInterval=" + stepInterval +
                ", recomputeFullDataInterval=" + recomputeFullDataInterval +
                '}';
    }

    public OrbitParameterConfiguration copy() {
        return new OrbitParameterConfiguration(this.beforePropagationSteps, this.afterPropagationSteps, this.stepInterval, this.recomputeFullDataInterval);
    }
}
