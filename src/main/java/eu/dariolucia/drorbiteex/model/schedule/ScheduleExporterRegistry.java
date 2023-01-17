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

import java.util.*;

public class ScheduleExporterRegistry {

    public static ScheduleExporterRegistry INSTANCE;

    public static ScheduleExporterRegistry instance() {
        synchronized (ScheduleExporterRegistry.class) {
            if(INSTANCE == null) {
                INSTANCE = new ScheduleExporterRegistry();
            }
        }
        return INSTANCE;
    }

    private final Map<String, IScheduleExporter> exporterMap = new TreeMap<>();
    private final Map<String, IScheduleNameGenerator> nameGeneratorMap = new TreeMap<>();

    private ScheduleExporterRegistry() {
        ServiceLoader.load(IScheduleExporter.class).stream().forEach(this::addExporter);
        ServiceLoader.load(IScheduleNameGenerator.class).stream().forEach(this::addGenerator);
    }

    private void addExporter(ServiceLoader.Provider<IScheduleExporter> o) {
        IScheduleExporter exporter = o.get();
        exporterMap.put(exporter.getName(), exporter);
    }

    private void addGenerator(ServiceLoader.Provider<IScheduleNameGenerator> o) {
        IScheduleNameGenerator generator = o.get();
        nameGeneratorMap.put(generator.getName(), generator);
    }

    public List<String> getExporters() {
        return new LinkedList<>(exporterMap.keySet());
    }

    public IScheduleExporter getExporter(String name) {
        return exporterMap.get(name);
    }


    public List<String> getNameGenerators() {
        return new LinkedList<>(nameGeneratorMap.keySet());
    }

    public IScheduleNameGenerator getNameGenerator(String name) {
        return nameGeneratorMap.get(name);
    }

}
