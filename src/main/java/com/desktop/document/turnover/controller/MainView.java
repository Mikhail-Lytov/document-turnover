package com.desktop.document.turnover.controller;

import com.desktop.document.turnover.DocumentTurnoverApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MainView {

    private final ApplicationContext applicationContext;

    @FXML
    private BorderPane root;

    @FXML
    private StackPane contentContainer;

    private Node converterView;
    private Node alphabetReplaceView;

    @FXML
    private void initialize() {
        converterButtonClick();
    }

    @FXML
    protected void converterButtonClick() {
        if (converterView == null) {
            converterView = loadView("ConverterView.fxml");
        }
        contentContainer.getChildren().setAll(converterView);
    }

    @FXML
    protected void searchByAlphabetButtonClick() {
        if (alphabetReplaceView == null) {
            alphabetReplaceView = loadView("AlphabetReplaceView.fxml");
        }
        contentContainer.getChildren().setAll(alphabetReplaceView);
    }

    private Node loadView(String fxmlName) {
        FXMLLoader loader = new FXMLLoader(DocumentTurnoverApplication.class.getResource(fxmlName));
        loader.setControllerFactory(applicationContext::getBean);

        try {
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load view: " + fxmlName, e);
        }
    }
}
