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

public class DefaultGenerator implements IScheduleNameGenerator {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    public DefaultGenerator() {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return "Default Generator";
    }

    @Override
    public String generateFileName(ScheduleGenerationRequest request, Date generationDate) {
        return request.getGroundStation().getCode() + "_" + dateFormatter.format(generationDate) + "_" + dateFormatter.format(request.getStartTime()) + "_" + dateFormatter.format(request.getEndTime()) + ".xml";
    }
}
