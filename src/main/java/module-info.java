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

    opens com.descktop.project.documentturnover.service to spring.beans, spring.core, spring.context;
    opens com.descktop.project.documentturnover to javafx.fxml, spring.core, spring.beans, spring.context;
    exports com.descktop.project.documentturnover;
}
