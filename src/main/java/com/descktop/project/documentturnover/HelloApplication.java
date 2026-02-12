package com.descktop.project.documentturnover;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class HelloApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(DocumentTurnoverSpringBootApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("MainView2.fxml"));
        fxmlLoader.setControllerFactory(applicationContext::getBean);

        BorderPane root = new BorderPane();
        fxmlLoader.setRoot(root);
        fxmlLoader.load();

        Scene scene = new Scene(root, 460, 480);
        scene.getStylesheets().add(
                HelloApplication.class.getResource("styles/app.css").toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
