package com.desktop.document.turnover;

import com.jacob.com.LibraryLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DocumentTurnoverApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, "C:\\Users\\Lytov\\Desktop\\document-turnover\\jacob-1.18-x64.dll");// TODO: Сделать динаический поиск
        launch(args);
    }

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(DocumentTurnoverApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(DocumentTurnoverApplication.class.getResource("MainView2.fxml"));
        fxmlLoader.setControllerFactory(applicationContext::getBean);

        BorderPane root = new BorderPane();
        fxmlLoader.setRoot(root);
        fxmlLoader.load();

        Scene scene = new Scene(root, 860, 480);
        scene.getStylesheets().add(
                DocumentTurnoverApplication.class.getResource("styles/app.css").toExternalForm()
        );
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(480);
        stage.show();
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
