package com.desktop.document.turnover.controller.section;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConverterViewController {

    private final ConverterSectionHandler converterSectionHandler;

    @FXML
    private TextField tfDirectory;

    @FXML
    private MenuButton typeFrom;

    @FXML
    private MenuButton typeTo;

    @FXML
    private Button startConverterButton;

    @FXML
    private TextField allFilesInformationParser;

    @FXML
    private TextField goodConverterInformationParser;

    @FXML
    private TextField oldVersionPathInformationParser;

    @FXML
    private TextField newVersionPathInformationParser;

    @FXML
    private Button openBackupPathButton;

    @FXML
    private Button openConvertedPathButton;

    @FXML
    private void initialize() {
        converterSectionHandler.init(typeFrom, typeTo, startConverterButton, tfDirectory);
        oldVersionPathInformationParser.textProperty().addListener((observable, oldValue, newValue) -> updateOpenPathButtonsState());
        newVersionPathInformationParser.textProperty().addListener((observable, oldValue, newValue) -> updateOpenPathButtonsState());
        updateOpenPathButtonsState();
    }

    @FXML
    protected void directorySelectedButtonClick() {
        Window owner = tfDirectory.getScene() != null ? tfDirectory.getScene().getWindow() : null;
        converterSectionHandler.handleDirectorySelection(tfDirectory, startConverterButton, owner);
    }

    @FXML
    protected void startConverterButtonClick() {
        converterSectionHandler.convert(
                tfDirectory,
                allFilesInformationParser,
                goodConverterInformationParser,
                oldVersionPathInformationParser,
                newVersionPathInformationParser
        );
        updateOpenPathButtonsState();
    }

    @FXML
    protected void openBackupPathButtonClick() {
        try {
            converterSectionHandler.openInFileManager(oldVersionPathInformationParser.getText(), "Путь к бекапу");
        } catch (Exception exception) {
            showError("открытие папки бекапа", exception);
        }
    }

    @FXML
    protected void openConvertedPathButtonClick() {
        try {
            converterSectionHandler.openInFileManager(newVersionPathInformationParser.getText(), "Путь к конвертированной папке");
        } catch (Exception exception) {
            showError("открытие конвертированной папки", exception);
        }
    }

    private void updateOpenPathButtonsState() {
        openBackupPathButton.setDisable(oldVersionPathInformationParser.getText() == null || oldVersionPathInformationParser.getText().isBlank());
        openConvertedPathButton.setDisable(newVersionPathInformationParser.getText() == null || newVersionPathInformationParser.getText().isBlank());
    }

    private void showError(String actionName, Throwable throwable) {
        String message = throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? "Неизвестная ошибка"
                : throwable.getMessage();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка выполнения");
        alert.setHeaderText("Не удалось выполнить " + actionName);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
