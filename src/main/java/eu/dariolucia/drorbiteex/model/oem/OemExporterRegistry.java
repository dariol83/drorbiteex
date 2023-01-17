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

import java.util.*;

public class OemExporterRegistry {

    public static OemExporterRegistry INSTANCE;

    public static OemExporterRegistry instance() {
        synchronized (OemExporterRegistry.class) {
            if(INSTANCE == null) {
                INSTANCE = new OemExporterRegistry();
            }
        }
        return INSTANCE;
    }

    private final Map<String, IOemPostProcessor> postProcessorMap = new TreeMap<>();
    private final Map<String, IOemNameGenerator> nameGeneratorMap = new TreeMap<>();

    private OemExporterRegistry() {
        ServiceLoader.load(IOemPostProcessor.class).stream().forEach(this::addPostProcessor);
        ServiceLoader.load(IOemNameGenerator.class).stream().forEach(this::addGenerator);
    }

    private void addPostProcessor(ServiceLoader.Provider<IOemPostProcessor> o) {
        IOemPostProcessor exporter = o.get();
        postProcessorMap.put(exporter.getName(), exporter);
    }

    private void addGenerator(ServiceLoader.Provider<IOemNameGenerator> o) {
        IOemNameGenerator generator = o.get();
        nameGeneratorMap.put(generator.getName(), generator);
    }

    public List<String> getPostProcessors() {
        return new LinkedList<>(postProcessorMap.keySet());
    }

    public IOemPostProcessor getPostProcessor(String name) {
        return postProcessorMap.get(name);
    }


    public List<String> getNameGenerators() {
        return new LinkedList<>(nameGeneratorMap.keySet());
    }

    public IOemNameGenerator getNameGenerator(String name) {
        return nameGeneratorMap.get(name);
    }

}
