module com.descktop.project.documentturnover {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires jakarta.annotation;
    requires static lombok;
    requires jacob;

    opens com.desktop.document.turnover.service.impl.converter to spring.beans, spring.core, spring.context;
    opens com.desktop.document.turnover to javafx.fxml, spring.core, spring.beans, spring.context;
    exports com.desktop.document.turnover;
    exports com.desktop.document.turnover.controller;
    opens com.desktop.document.turnover.controller to javafx.fxml, spring.beans, spring.context, spring.core;
    exports com.desktop.document.turnover.controller.section;
    opens com.desktop.document.turnover.controller.section to javafx.fxml, spring.beans, spring.context, spring.core;

    exports com.desktop.document.turnover.configuration;
    opens  com.desktop.document.turnover.configuration to javafx.fxml, spring.beans, spring.context, spring.core;
}
