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

    exports eu.dariolucia.drorbiteex.fxml;
    exports eu.dariolucia.drorbiteex.application;
    exports eu.dariolucia.drorbiteex.data;
}