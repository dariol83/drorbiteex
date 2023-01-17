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

package eu.dariolucia.drorbiteex.model.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ScheduledActivityParameter {

    public static ScheduledActivityParameter timeParameterB(String name, Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new ScheduledActivityParameter("timeParameterB", name, sdf.format(value));
    }

    public static ScheduledActivityParameter timeParameterA(String name, Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new ScheduledActivityParameter("timeParameterA", name, sdf.format(value));
    }

    public static ScheduledActivityParameter booleanParameter(String name, boolean value) {
        return new ScheduledActivityParameter("booleanParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter intParameter(String name, int value) {
        return new ScheduledActivityParameter("intParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter unsignedIntParameter(String name, long value) {
        return new ScheduledActivityParameter("unsignedIntParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter floatParameter(String name, float value) {
        return new ScheduledActivityParameter("floatParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter doubleParameter(String name, double value) {
        return new ScheduledActivityParameter("doubleParameter", name, String.valueOf(value));
    }

    public static ScheduledActivityParameter stringParameter(String name, String value) {
        return new ScheduledActivityParameter("stringParameter", name, value);
    }

    private final String tagName;
    private final String parameterName;
    private final String parameterValue;

    private ScheduledActivityParameter(String tagName, String parameterName, String parameterValue) {
        this.tagName = tagName;
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getTagName() {
        return tagName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }
}
