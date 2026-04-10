package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AlphabetReplaceSectionHandler {

    private final AlphabetReplaceService alphabetReplaceService;

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

    public Path selectAlphabetImportFile(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл алфавита замен");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File selected = chooser.showOpenDialog(owner);
        return selected != null ? selected.toPath() : null;
    }

    public Path selectAlphabetExportFile(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить алфавит замен");
        chooser.setInitialFileName("alphabet-replacements.txt");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File selected = chooser.showSaveDialog(owner);
        return selected != null ? selected.toPath() : null;
    }

    public String importAlphabet(Path alphabetFile) {
        Path path = parseFile(alphabetFile != null ? alphabetFile.toString() : null, "Файл алфавита замен");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать файл алфавита: " + exception.getMessage(), exception);
        }
    }

    public Path saveAlphabet(Path targetFile, String alphabetContent) {
        if (targetFile == null) {
            throw new IllegalArgumentException("Не выбран файл для сохранения алфавита.");
        }

        String normalizedContent = normalizeAlphabetContent(alphabetContent);
        Path output = ensureTxtExtension(targetFile);
        Path parent = output.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, normalizedContent, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сохранить алфавит: " + exception.getMessage(), exception);
        }
        return output;
    }

    public void copyAlphabetToClipboard(String alphabetContent) {
        String normalizedContent = normalizeAlphabetContent(alphabetContent);
        ClipboardContent content = new ClipboardContent();
        content.putString(normalizedContent);
        Clipboard.getSystemClipboard().setContent(content);
    }

    public AlphabetReplaceService.ReplaceOperationResult replace(String directoryPath, String alphabetContent) {
        Path directory = parseDirectory(directoryPath, "Папка с документами");
        String normalizedAlphabetContent = normalizeAlphabetContent(alphabetContent);
        return alphabetReplaceService.replaceInDocuments(directory, normalizedAlphabetContent);
    }

    public AlphabetReplaceService.SearchOperationResult search(String directoryPath, String searchText) {
        Path directory = parseDirectory(directoryPath, "Папка для поиска");
        String text = normalizeSearchText(searchText);
        return alphabetReplaceService.searchInDocuments(directory, text);
    }

    public void openInFileManager(String pathValue) {
        Path path = parseExistingPath(pathValue, "Путь к бэкапу").toAbsolutePath().normalize();
        boolean isDirectory = Files.isDirectory(path);
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        try {
            if (osName.contains("win")) {
                if (isDirectory) {
                    new ProcessBuilder("explorer", path.toString()).start();
                } else {
                    new ProcessBuilder("explorer", "/select," + path).start();
                }
                return;
            }

            if (osName.contains("mac")) {
                if (isDirectory) {
                    new ProcessBuilder("open", path.toString()).start();
                } else {
                    new ProcessBuilder("open", "-R", path.toString()).start();
                }
                return;
            }

            Path target = isDirectory ? path : path.getParent();
            if (target == null) {
                throw new IllegalArgumentException("Не удалось определить папку для открытия: " + path);
            }
            new ProcessBuilder("xdg-open", target.toString()).start();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось открыть путь в файловом менеджере: " + exception.getMessage(), exception);
        }
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

    private Path parseExistingPath(String pathValue, String fieldName) {
        if (pathValue == null || pathValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не указан.");
        }

        Path path = Path.of(pathValue.trim());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Путь не существует: " + path);
        }

        return path;
    }

    private String normalizeSearchText(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            throw new IllegalArgumentException("Текст для поиска не может быть пустым.");
        }
        return searchText.trim();
    }

    private String normalizeAlphabetContent(String alphabetContent) {
        if (alphabetContent == null || alphabetContent.isBlank()) {
            throw new IllegalArgumentException("Алфавит замен не может быть пустым.");
        }
        return alphabetContent.replace("\r\n", "\n").trim();
    }

    private Path ensureTxtExtension(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".txt")) {
            return path;
        }
        return path.resolveSibling(fileName + ".txt");
    }

    private void updateSearchButtonState(TextField searchDirectoryField, TextField searchTextField, Button startSearchButton) {
        boolean disabled = searchDirectoryField.getText().isBlank() || searchTextField.getText().isBlank();
        startSearchButton.setDisable(disabled);
    }
}
