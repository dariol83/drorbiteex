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

package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.util.TimeUtils;

import java.text.ParseException;
import java.time.Instant;

public class NumberToDateAxisFormatter extends javafx.util.StringConverter<Number> {

    @Override
    public String toString(Number number) {
        return TimeUtils.formatDate(Instant.ofEpochMilli(number.longValue()));
    }

    @Override
    public Number fromString(String s) {
        try {
            return TimeUtils.parseDateToInstant(s).toEpochMilli();
        } catch (ParseException e) {
            return Double.NaN; // Unparseable
        }
    }
}
