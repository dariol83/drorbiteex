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

    public static ErrorPoint noVisibility(Instant time) {
        return new ErrorPoint(time, -1, -1);
    }

    private final Instant time;
    private final double error1;
    private final double error2;

    public ErrorPoint(Instant time, double error1, double error2) {
        this.time = time;
        this.error1 = error1;
        this.error2 = error2;
    }

    public Instant getTime() {
        return time;
    }

    public double getError1() {
        return error1;
    }

    public double getError2() {
        return error2;
    }

    public boolean isNoVisibility() {
        return error1 == -1 && error2 == -1;
    }
}
