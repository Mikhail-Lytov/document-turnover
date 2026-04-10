package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class AlphabetReplaceSectionHandler {

    private final AlphabetReplaceService alphabetReplaceService;

    public void initReplace(TextField replaceDirectoryField, TextField alphabetFileField, Button startReplaceButton) {
        replaceDirectoryField.textProperty().addListener((observable, oldValue, newValue) ->
                updateReplaceButtonState(replaceDirectoryField, alphabetFileField, startReplaceButton)
        );
        alphabetFileField.textProperty().addListener((observable, oldValue, newValue) ->
                updateReplaceButtonState(replaceDirectoryField, alphabetFileField, startReplaceButton)
        );

        updateReplaceButtonState(replaceDirectoryField, alphabetFileField, startReplaceButton);
    }

    public void initSearch(TextField searchDirectoryField, TextField searchTextField, Button startSearchButton) {
        searchDirectoryField.textProperty().addListener((observable, oldValue, newValue) ->
                updateSearchButtonState(searchDirectoryField, searchTextField, startSearchButton)
        );
        searchTextField.textProperty().addListener((observable, oldValue, newValue) ->
                updateSearchButtonState(searchDirectoryField, searchTextField, startSearchButton)
        );

        updateSearchButtonState(searchDirectoryField, searchTextField, startSearchButton);
    }

    public void selectDirectory(TextField targetField, Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку");

        File selected = chooser.showDialog(owner);
        if (selected != null) {
            targetField.setText(selected.getAbsolutePath());
        }
    }

    public void selectAlphabetFile(TextField targetField, Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл алфавита замен");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            targetField.setText(selected.getAbsolutePath());
        }
    }

    public AlphabetReplaceService.ReplaceOperationResult replace(String directoryPath, String alphabetFilePath) {
        Path directory = parseDirectory(directoryPath, "Папка с документами");
        Path alphabetFile = parseFile(alphabetFilePath, "Файл алфавита замен");
        return alphabetReplaceService.replaceInDocuments(directory, alphabetFile);
    }

    public AlphabetReplaceService.SearchOperationResult search(String directoryPath, String searchText) {
        Path directory = parseDirectory(directoryPath, "Папка для поиска");
        String text = normalizeSearchText(searchText);
        return alphabetReplaceService.searchInDocuments(directory, text);
    }

    private Path parseDirectory(String directoryPath, String fieldName) {
        if (directoryPath == null || directoryPath.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не указана.");
        }

        Path path = Path.of(directoryPath.trim());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Путь не существует: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Ожидается папка, а не файл: " + path);
        }

        return path;
    }

    private Path parseFile(String filePath, String fieldName) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не указан.");
        }

        Path path = Path.of(filePath.trim());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Файл не найден: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Ожидается файл: " + path);
        }

        return path;
    }

    private String normalizeSearchText(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            throw new IllegalArgumentException("Текст для поиска не может быть пустым.");
        }
        return searchText.trim();
    }

    private void updateReplaceButtonState(TextField replaceDirectoryField, TextField alphabetFileField, Button startReplaceButton) {
        boolean disabled = replaceDirectoryField.getText().isBlank() || alphabetFileField.getText().isBlank();
        startReplaceButton.setDisable(disabled);
    }

    private void updateSearchButtonState(TextField searchDirectoryField, TextField searchTextField, Button startSearchButton) {
        boolean disabled = searchDirectoryField.getText().isBlank() || searchTextField.getText().isBlank();
        startSearchButton.setDisable(disabled);
    }
}
