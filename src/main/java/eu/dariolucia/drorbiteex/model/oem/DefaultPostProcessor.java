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

import java.util.Date;

public class DefaultPostProcessor implements IOemPostProcessor {

    public static final String POST_PROCESSOR_NAME = "No post-processing";

    @Override
    public String getName() {
        return POST_PROCESSOR_NAME;
    }

    @Override
    public void postProcess(String filePath, OemGenerationRequest request, Date generationDate) {
    }
}
