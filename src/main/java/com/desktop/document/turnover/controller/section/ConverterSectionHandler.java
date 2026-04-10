package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.domain.enums.ResourcesSystemType;
import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import com.desktop.document.turnover.service.impl.converter.ConverterStrategy;
import com.desktop.document.turnover.utils.GenerateMenuButton;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConverterSectionHandler {

    private static final String APP_STYLESHEET_PATH = "/com/desktop/document/turnover/styles/app.css";
    private static final String LIGHT_THEME_CLASS = "theme-light";
    private static final String DARK_THEME_CLASS = "theme-dark";

    private final ConverterStrategy converterStrategy;
    private TypeFromDocs selectedTypeFrom;
    private TypeToDocs selectedTypeTo;
    private ResourcesSystemType selectedResourceType = ResourcesSystemType.DIRECTORY;

    public void init(MenuButton typeFrom, MenuButton typeTo, Button startConverterButton, TextField tfDirectory) {
        initTypeFrom(typeFrom, startConverterButton, tfDirectory);
        initTypeTo(typeTo, startConverterButton, tfDirectory);
        updateStartConverterButtonState(startConverterButton, tfDirectory);
    }

    public void show(VBox convertBox, VBox informationBox) {
        convertBox.setVisible(true);
        informationBox.setVisible(true);
    }

    public void handleDirectorySelection(TextField tfDirectory, Button startConverterButton, Window owner) {
        Optional<ResourcesSystemType> selectedType = showSourceTypeDialog(owner);
        if (selectedType.isEmpty()) {
            return;
        }

        selectedResourceType = selectedType.get();

        File selected = ResourcesSystemType.FILE.equals(selectedResourceType)
                ? showFileChooser(owner, tfDirectory.getText())
                : showDirectoryChooser(owner, tfDirectory.getText());

        if (selected != null) {
            tfDirectory.setText(selected.getAbsolutePath());
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        }
    }

    public void convert(
            TextField tfDirectory,
            TextField allFilesInformationParser,
            TextField goodConverterInformationParser,
            TextField oldVersionPathInformationParser,
            TextField newVersionPathInformationParser
    ) {
        if (selectedTypeFrom == null || selectedTypeTo == null || tfDirectory.getText().isBlank()) {
            return;
        }

        Path selectedDirectory = Path.of(tfDirectory.getText());
        ConverterService converterService = converterStrategy.getConverterService(selectedTypeFrom);

        int allFiles = converterService.getFilesCountByType(selectedDirectory, selectedTypeFrom);
        int convertedFiles = converterService.convertDirectory(selectedDirectory, selectedTypeFrom, selectedTypeTo);

        allFilesInformationParser.setText(String.valueOf(allFiles));
        goodConverterInformationParser.setText(String.valueOf(convertedFiles));
        oldVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
        newVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
    }

    public void openInFileManager(String pathValue, String fieldName) {
        Path path = parseExistingPath(pathValue, fieldName).toAbsolutePath().normalize();
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

    private void initTypeFrom(MenuButton typeFrom, Button startConverterButton, TextField tfDirectory) {
        GenerateMenuButton.generateMenuButton(typeFrom, TypeFromDocs.class, selected -> {
            typeFrom.setText(selected.name());
            selectedTypeFrom = selected;
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        });
    }

    private void initTypeTo(MenuButton typeTo, Button startConverterButton, TextField tfDirectory) {
        GenerateMenuButton.generateMenuButton(typeTo, TypeToDocs.class, selected -> {
            typeTo.setText(selected.name());
            selectedTypeTo = selected;
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        });
    }

    private void updateStartConverterButtonState(Button startConverterButton, TextField tfDirectory) {
        if (startConverterButton == null) {
            return;
        }

        boolean directoryIsBlank = tfDirectory == null || tfDirectory.getText().isBlank();
        startConverterButton.setDisable(selectedTypeFrom == null || selectedTypeTo == null || directoryIsBlank);
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

    private Optional<ResourcesSystemType> showSourceTypeDialog(Window owner) {
        Dialog<ResourcesSystemType> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Выбор источника");
        dialog.setHeaderText("Выберите тип источника");

        DialogPane dialogPane = dialog.getDialogPane();
        applyDialogStyles(dialogPane, owner);

        ToggleGroup typeToggleGroup = new ToggleGroup();
        ToggleButton directoryButton = createSourceTypeToggleButton("Директория", ResourcesSystemType.DIRECTORY, typeToggleGroup);
        ToggleButton fileButton = createSourceTypeToggleButton("Файл", ResourcesSystemType.FILE, typeToggleGroup);

        if (ResourcesSystemType.FILE.equals(selectedResourceType)) {
            fileButton.setSelected(true);
        } else {
            directoryButton.setSelected(true);
        }

        Label description = new Label("Что хотите выбрать?");
        description.getStyleClass().add("section-label");

        HBox toggleButtons = new HBox(8.0, directoryButton, fileButton);
        HBox.setHgrow(directoryButton, Priority.ALWAYS);
        HBox.setHgrow(fileButton, Priority.ALWAYS);

        VBox content = new VBox(10.0, description, toggleButtons);
        content.getStyleClass().add("source-type-dialog-content");
        content.setPadding(new Insets(4.0, 0.0, 0.0, 0.0));
        dialogPane.setContent(content);

        ButtonType chooseButtonType = new ButtonType("Выбрать", ButtonBar.ButtonData.OK_DONE);
        ButtonType closesButtonType = new ButtonType("Отмена", ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().setAll(chooseButtonType, closesButtonType);
        dialogPane.lookupButton(chooseButtonType).getStyleClass().add("dialog-primary-button");

        dialog.setResultConverter(buttonType -> {
            if (buttonType != chooseButtonType || typeToggleGroup.getSelectedToggle() == null) {
                return null;
            }
            return (ResourcesSystemType) typeToggleGroup.getSelectedToggle().getUserData();
        });

        return dialog.showAndWait();
    }

    private ToggleButton createSourceTypeToggleButton(String title, ResourcesSystemType sourceType, ToggleGroup toggleGroup) {
        ToggleButton button = new ToggleButton(title);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll("theme-button", "source-type-toggle");
        button.setToggleGroup(toggleGroup);
        button.setUserData(sourceType);
        return button;
    }

    private File showFileChooser(Window owner, String currentPath) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        applyInitialDirectory(fileChooser, currentPath);
        return fileChooser.showOpenDialog(owner);
    }

    private File showDirectoryChooser(Window owner, String currentPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку");
        applyInitialDirectory(directoryChooser, currentPath);
        return directoryChooser.showDialog(owner);
    }

    private void applyInitialDirectory(FileChooser chooser, String currentPath) {
        File initialDirectory = resolveInitialDirectory(currentPath);
        if (initialDirectory == null) {
            return;
        }

        try {
            chooser.setInitialDirectory(initialDirectory);
        } catch (IllegalArgumentException ignored) {
            // ignore invalid initial directory and let chooser use default
        }
    }

    private void applyInitialDirectory(DirectoryChooser chooser, String currentPath) {
        File initialDirectory = resolveInitialDirectory(currentPath);
        if (initialDirectory == null) {
            return;
        }

        try {
            chooser.setInitialDirectory(initialDirectory);
        } catch (IllegalArgumentException ignored) {
            // ignore invalid initial directory and let chooser use default
        }
    }

    private File resolveInitialDirectory(String currentPath) {
        if (currentPath == null || currentPath.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(currentPath.trim()).toAbsolutePath().normalize();
            Path directory = Files.isDirectory(path) ? path : path.getParent();
            if (directory == null || !Files.isDirectory(directory)) {
                return null;
            }
            return directory.toFile();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void applyDialogStyles(DialogPane dialogPane, Window owner) {
        var stylesheet = getClass().getResource(APP_STYLESHEET_PATH);
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }

        dialogPane.getStyleClass().add("app-root");
        dialogPane.getStyleClass().add("source-type-dialog");

        String themeClass = resolveThemeClass(owner);
        dialogPane.getStyleClass().add(themeClass != null ? themeClass : DARK_THEME_CLASS);
    }

    private String resolveThemeClass(Window owner) {
        if (owner == null) {
            return null;
        }

        Scene scene = owner.getScene();
        if (scene == null || scene.getRoot() == null) {
            return null;
        }

        if (scene.getRoot().getStyleClass().contains(LIGHT_THEME_CLASS)) {
            return LIGHT_THEME_CLASS;
        }
        if (scene.getRoot().getStyleClass().contains(DARK_THEME_CLASS)) {
            return DARK_THEME_CLASS;
        }

        return null;
    }
}
