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

package eu.dariolucia.drorbiteex.model.oem;

import org.orekit.files.ccsds.utils.FileFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DefaultGenerator implements IOemNameGenerator {

    public static final String GENERATOR_NAME = "Default Generator";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

    public DefaultGenerator() {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return GENERATOR_NAME;
    }

    @Override
    public String generateFileName(OemGenerationRequest request, Date generationDate) {
        String suffix = request.getFormat() == FileFormat.XML ? "xml" : "txt";
        return request.getCode() + "_OEM_" + dateFormatter.format(generationDate) + "_" + dateFormatter.format(request.getStartTime()) + "_" + dateFormatter.format(request.getEndTime()) + "." + suffix;
    }
}
