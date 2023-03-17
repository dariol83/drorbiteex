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

import java.time.Instant;

public class ErrorPoint {

    private final Instant time;
    private final double[] errors;

    public ErrorPoint(Instant time, double... errors) {
        this.time = time;
        this.errors = errors;
    }

    public Instant getTime() {
        return time;
    }

    public double[] getErrors() {
        return errors;
    }

    public double getErrorAt(int idx) {
        return errors[idx];
    }

    public int getNbErrors() {
        return errors.length;
    }
}
