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

import java.util.Date;

/**
 * Name generator implementation for CCSDS Simple Schedule files.
 */
public interface IScheduleNameGenerator {
    /**
     * Name of the implementation.
     *
     * @return implementation name
     */
    String getName();

    /**
     * Method called to request the generation of a file name, linked to the provided request.
     *
     * @param request the request
     * @param generationDate the generation date of the file
     * @return the file name to be used
     */
    String generateFileName(ScheduleGenerationRequest request, Date generationDate);
}
