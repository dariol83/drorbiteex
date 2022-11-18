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

    private ScheduleExporterRegistry() {
        ServiceLoader.load(IScheduleExporter.class).stream().forEach(this::addExporter);
    }

    private void addExporter(ServiceLoader.Provider<IScheduleExporter> o) {
        IScheduleExporter exporter = o.get();
        exporterMap.put(exporter.getName(), exporter);
    }

    public List<String> getExporters() {
        return new LinkedList<>(exporterMap.keySet());
    }

    public IScheduleExporter getExporter(String name) {
        return exporterMap.get(name);
    }

}
