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

package eu.dariolucia.drorbiteex.model.oem;

import java.util.Date;

/**
 * Post-processor for CCSDS OEM files. The postProcess() method will be invoked after the full generation of the OEM file.
 */
public interface IOemPostProcessor {
    /**
     * Name of the implementation.
     *
     * @return implementation name
     */
    String getName();

    /**
     * Method that requests the execution of post-processing actions to the generated OEM file.
     * @param filePath absolute path of the CCSDS OEM file
     * @param request generation request
     * @param generationDate generation date
     */
    void postProcess(String filePath, OemGenerationRequest request, Date generationDate);
}
