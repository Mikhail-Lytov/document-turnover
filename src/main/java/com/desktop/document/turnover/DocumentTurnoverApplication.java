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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@SpringBootApplication
public class DocumentTurnoverApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) throws IOException {
        String dllPath = extractJacobDllFromResources();
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, dllPath);
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

    private static String extractJacobDllFromResources() throws IOException {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean is64 = arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64");

        String resourceName = is64
                ? "/com/desktop/document/turnover/native/jacob-1.18-x64.dll"
                : "/native/jacob-1.18-x86.dll";

        try (InputStream in = DocumentTurnoverApplication.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException("Ресурс не найден: " + resourceName);
            }

            Path temp = Files.createTempFile("jacob-", ".dll");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toAbsolutePath().toString();
        }
    }
}
