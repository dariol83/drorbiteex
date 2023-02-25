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

package eu.dariolucia.drorbiteex.model.util;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    public static AbsoluteDate toAbsoluteDate(Date time) {
        return new AbsoluteDate(time, TimeScalesFactory.getUTC());
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public static String formatDate(Date d) {
        if(d == null) {
            return "N/A";
        }
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(d);
    }

    public static String formatDate(Instant time) {
        Date d = time != null ? new Date(time.toEpochMilli()) : null;
        return formatDate(d);
    }

    public static Date parseDate(String s) throws ParseException {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.parse(s);
    }

    public static Instant parseDateToInstant(String s) throws ParseException {
        return parseDate(s).toInstant();
    }
}
