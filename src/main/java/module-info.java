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
    exports eu.dariolucia.drorbiteex.application;
    exports eu.dariolucia.drorbiteex.model;
    exports eu.dariolucia.drorbiteex.model.orbit;
    exports eu.dariolucia.drorbiteex.model.station;
    exports eu.dariolucia.drorbiteex.model.util;
    exports eu.dariolucia.drorbiteex.model.schedule;

    uses eu.dariolucia.drorbiteex.model.schedule.IScheduleExporter;
    provides eu.dariolucia.drorbiteex.model.schedule.IScheduleExporter with eu.dariolucia.drorbiteex.model.schedule.DefaultExporter;

}