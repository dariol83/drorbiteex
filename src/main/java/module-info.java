open module eu.dariolucia.drorbiteex {
    requires java.logging;

    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;

    requires java.xml.bind;
    requires com.sun.xml.bind;

    requires org.orekit;
    requires hipparchus.geometry;
    requires hipparchus.ode;

    exports eu.dariolucia.drorbiteex.fxml;
    exports eu.dariolucia.drorbiteex.fxml.progress;
    exports eu.dariolucia.drorbiteex.fxml.range;
    exports eu.dariolucia.drorbiteex.fxml.canvas;
    exports eu.dariolucia.drorbiteex.application;
    exports eu.dariolucia.drorbiteex.model;
    exports eu.dariolucia.drorbiteex.model.orbit;
    exports eu.dariolucia.drorbiteex.model.station;
    exports eu.dariolucia.drorbiteex.model.util;
    exports eu.dariolucia.drorbiteex.model.schedule;
    exports eu.dariolucia.drorbiteex.model.oem;
    exports eu.dariolucia.drorbiteex.model.tle;
    exports eu.dariolucia.drorbiteex.model.collinearity;

    uses eu.dariolucia.drorbiteex.model.schedule.IScheduleExporter;
    uses eu.dariolucia.drorbiteex.model.schedule.IScheduleNameGenerator;
    uses eu.dariolucia.drorbiteex.model.oem.IOemPostProcessor;
    uses eu.dariolucia.drorbiteex.model.oem.IOemNameGenerator;

    provides eu.dariolucia.drorbiteex.model.schedule.IScheduleExporter with eu.dariolucia.drorbiteex.model.schedule.DefaultExporter;
    provides eu.dariolucia.drorbiteex.model.schedule.IScheduleNameGenerator with eu.dariolucia.drorbiteex.model.schedule.DefaultGenerator;
    provides eu.dariolucia.drorbiteex.model.oem.IOemPostProcessor with eu.dariolucia.drorbiteex.model.oem.DefaultPostProcessor;
    provides eu.dariolucia.drorbiteex.model.oem.IOemNameGenerator with eu.dariolucia.drorbiteex.model.oem.DefaultGenerator;
}