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

import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

import java.time.Instant;
import java.util.Date;

public abstract class Measurement {

    // TODO: add sigma, weight
    private final Instant time;

    protected Measurement(Instant time) {
        this.time = time;
    }

    public abstract String getInfo();

    public abstract Type getType();

    public Instant getTime() {
        return time;
    }

    public AbsoluteDate getAbsoluteDate() {
        return TimeUtils.toAbsoluteDate(new Date(getTime().toEpochMilli()));
    }

    public abstract AbstractMeasurement<?> toOrekitMeasurement(ObservableSatellite satellite, Frame orbitFrame);

    public enum Type {
        RANGE,
        POSITION,
        AZ_EL
    }
}
