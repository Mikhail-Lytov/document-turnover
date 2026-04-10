package com.desktop.document.turnover.controller;

import com.desktop.document.turnover.DocumentTurnoverApplication;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MainView {

    private static final String LIGHT_THEME_CLASS = "theme-light";
    private static final String DARK_THEME_CLASS = "theme-dark";
    private static final Duration CONTENT_ANIMATION_DURATION = Duration.millis(180);

    private final ApplicationContext applicationContext;

    @FXML
    private BorderPane root;

    @FXML
    private StackPane contentContainer;

    @FXML
    private ToggleButton converterNavButton;

    @FXML
    private ToggleButton alphabetNavButton;

    @FXML
    private ToggleButton searchDocumentsNavButton;

    @FXML
    private ToggleButton lightThemeButton;

    @FXML
    private ToggleButton darkThemeButton;

    private final ToggleGroup sectionToggleGroup = new ToggleGroup();
    private final ToggleGroup themeToggleGroup = new ToggleGroup();

    private Node converterView;
    private Node alphabetReplaceView;
    private Node searchDocumentsView;

    @FXML
    private void initialize() {
        setupSectionNavigation();
        setupThemeSwitcher();
        applyTheme(DARK_THEME_CLASS);
        converterButtonClick();
    }

    @FXML
    protected void converterButtonClick() {
        if (converterView == null) {
            converterView = loadView("ConverterView.fxml");
        }
        converterNavButton.setSelected(true);
        showContent(converterView);
    }

    @FXML
    protected void searchByAlphabetButtonClick() {
        if (alphabetReplaceView == null) {
            alphabetReplaceView = loadView("AlphabetReplaceView.fxml");
        }
        alphabetNavButton.setSelected(true);
        showContent(alphabetReplaceView);
    }

    @FXML
    protected void searchDocumentsButtonClick() {
        if (searchDocumentsView == null) {
            searchDocumentsView = loadView("SearchDocumentsView.fxml");
        }
        searchDocumentsNavButton.setSelected(true);
        showContent(searchDocumentsView);
    }

    @FXML
    protected void lightThemeButtonClick() {
        applyTheme(LIGHT_THEME_CLASS);
    }

    @FXML
    protected void darkThemeButtonClick() {
        applyTheme(DARK_THEME_CLASS);
    }

    private void setupSectionNavigation() {
        converterNavButton.setToggleGroup(sectionToggleGroup);
        alphabetNavButton.setToggleGroup(sectionToggleGroup);
        searchDocumentsNavButton.setToggleGroup(sectionToggleGroup);
    }

    private void setupThemeSwitcher() {
        lightThemeButton.setToggleGroup(themeToggleGroup);
        darkThemeButton.setToggleGroup(themeToggleGroup);
        darkThemeButton.setSelected(true);
    }

    private void applyTheme(String themeClass) {
        root.getStyleClass().removeAll(LIGHT_THEME_CLASS, DARK_THEME_CLASS);
        root.getStyleClass().add(themeClass);
    }

    private void showContent(Node view) {
        contentContainer.getChildren().setAll(view);

        view.setOpacity(0);
        view.setTranslateY(12);

        FadeTransition fadeTransition = new FadeTransition(CONTENT_ANIMATION_DURATION, view);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        TranslateTransition translateTransition = new TranslateTransition(CONTENT_ANIMATION_DURATION, view);
        translateTransition.setFromY(12);
        translateTransition.setToY(0);

        new ParallelTransition(fadeTransition, translateTransition).play();
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
