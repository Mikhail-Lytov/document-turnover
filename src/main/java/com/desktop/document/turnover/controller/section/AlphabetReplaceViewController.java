package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import com.desktop.document.turnover.service.api.word.WordComparisonService;
import com.desktop.document.turnover.service.api.word.WordComparisonService.ComparisonTarget;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlphabetReplaceViewController {

    private static final String LIGHT_THEME_CLASS = "theme-light";
    private static final String DARK_THEME_CLASS = "theme-dark";
    private static final String REPLACE_PROGRESS_DEFAULT_TEXT = "Идет замена...";
    private static final String REPLACE_PROGRESS_TEXT_TEMPLATE = "Идет замена... %d%%";
    private static final PseudoClass NEW_ROW_PSEUDO_CLASS = PseudoClass.getPseudoClass("new-row");
    private static final double ALPHABET_EDITOR_MIN_ROW_HEIGHT = 52.0;
    private static final double ALPHABET_EDITOR_LINE_HEIGHT = 20.0;
    private static final double ALPHABET_EDITOR_ROW_VERTICAL_PADDING = 22.0;
    private static final double ALPHABET_EDITOR_CELL_HORIZONTAL_PADDING = 28.0;
    private static final double ALPHABET_EDITOR_AVERAGE_CHAR_WIDTH = 7.5;

    private final AlphabetReplaceSectionHandler alphabetReplaceSectionHandler;
    private final WordComparisonService wordComparisonService;

    @FXML
    private TextField replaceDirectoryField;

    @FXML
    private TextField alphabetSummaryField;

    @FXML
    private Button openAlphabetEditorButton;

    @FXML
    private Button startReplaceButton;

    @FXML
    private Button replaceDirectorySelectedButton;

    @FXML
    private ProgressIndicator replaceProgressIndicator;

    @FXML
    private Label replaceProgressLabel;

    @FXML
    private TextField replaceFilesCountField;

    @FXML
    private TextField replaceRulesCountField;

    @FXML
    private TextField replaceTotalCountField;

    @FXML
    private TextField backupPathField;

    @FXML
    private Button openBackupPathButton;

    @FXML
    private TextField logPathField;

    @FXML
    private Button openLogPathButton;

    @FXML
    private TextField contextLogPathField;

    @FXML
    private Button openContextLogPathButton;

    @FXML
    private Button openComparisonDialogButton;

    @FXML
    private TextArea outputArea;

    private boolean busy;
    private String alphabetContent = "";
    private List<ComparisonTarget> comparisonTargets = List.of();

    @FXML
    private void initialize() {
        replaceDirectoryField.textProperty().addListener((observable, oldValue, newValue) -> updateActionButtons());
        updateAlphabetSummary();
        updateActionButtons();
        outputArea.setText("Готово к работе. Выберите папку и заполните алфавит замен в отдельном редакторе.");
    }

    @FXML
    protected void replaceDirectorySelectedButtonClick() {
        Window owner = replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null;
        alphabetReplaceSectionHandler.selectDirectory(replaceDirectoryField, owner);
    }

    @FXML
    protected void openAlphabetEditorButtonClick() {
        if (busy) {
            return;
        }
        showAlphabetEditorDialog();
    }

    @FXML
    protected void startReplaceButtonClick() {
        clearReplaceResultFields();
        updateActionButtons();
        runReplaceTask();
    }

    @FXML
    protected void openBackupPathButtonClick() {
        try {
            alphabetReplaceSectionHandler.openInFileManager(backupPathField.getText(), "Путь к бэкапу");
        } catch (Exception exception) {
            showError("открытие бэкапа", exception);
        }
    }

    @FXML
    protected void openLogPathButtonClick() {
        try {
            alphabetReplaceSectionHandler.openInFileManager(logPathField.getText(), "Путь к основному логу");
        } catch (Exception exception) {
            showError("открытие основного лога", exception);
        }
    }

    @FXML
    protected void openContextLogPathButtonClick() {
        try {
            alphabetReplaceSectionHandler.openInFileManager(contextLogPathField.getText(), "Путь к логу контекстов");
        } catch (Exception exception) {
            showError("открытие лога контекстов", exception);
        }
    }

    @FXML
    protected void openComparisonDialogButtonClick() {
        if (comparisonTargets.isEmpty()) {
            showWarning(replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null,
                    "Нет измененных Word-файлов для сравнения.");
            return;
        }

        showComparisonDialog();
    }

    private void runReplaceTask() {
        setBusy(true);

        Task<AlphabetReplaceService.ReplaceOperationResult> task = new Task<>() {
            @Override
            protected AlphabetReplaceService.ReplaceOperationResult call() {
                updateProgress(0, 1);
                updateMessage(formatReplaceProgressMessage(0));

                AlphabetReplaceService.ReplaceOperationResult result = alphabetReplaceSectionHandler.replace(
                        replaceDirectoryField.getText(),
                        alphabetContent,
                        (processedFiles, totalFiles) -> {
                            if (totalFiles <= 0) {
                                updateProgress(1, 1);
                                updateMessage(formatReplaceProgressMessage(100));
                                return;
                            }

                            int boundedProcessedFiles = Math.min(totalFiles, Math.max(0, processedFiles));
                            int progressPercent = (int) Math.round((boundedProcessedFiles * 100.0) / totalFiles);
                            updateProgress(boundedProcessedFiles, totalFiles);
                            updateMessage(formatReplaceProgressMessage(progressPercent));
                        }
                );

                updateProgress(1, 1);
                updateMessage(formatReplaceProgressMessage(100));
                return result;
            }
        };

        replaceProgressIndicator.progressProperty().bind(task.progressProperty());
        replaceProgressLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            unbindReplaceProgressState();
            setBusy(false);
            applyReplaceResult(task.getValue());
        });

        task.setOnFailed(event -> {
            unbindReplaceProgressState();
            setBusy(false);
            showError("замену", task.getException());
        });

        Thread worker = new Thread(task, "alphabet-replace-replacement");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyReplaceResult(AlphabetReplaceService.ReplaceOperationResult result) {
        replaceFilesCountField.setText(String.valueOf(result.filesScanned()));
        replaceRulesCountField.setText(String.valueOf(result.rulesLoaded()));
        replaceTotalCountField.setText(String.valueOf(result.totalReplacements()));
        backupPathField.setText(result.backupPath() != null ? result.backupPath().toAbsolutePath().toString() : "");
        logPathField.setText(result.logFile() != null ? result.logFile().toAbsolutePath().toString() : "");
        contextLogPathField.setText(result.contextLogFile() != null ? result.contextLogFile().toAbsolutePath().toString() : "");
        outputArea.setText(result.report());
        comparisonTargets = buildComparisonTargets(result);
        updateActionButtons();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        if (busy) {
            replaceProgressIndicator.setProgress(0);
            replaceProgressLabel.setText(formatReplaceProgressMessage(0));
            outputArea.setText(REPLACE_PROGRESS_DEFAULT_TEXT);
        }

        replaceDirectoryField.setDisable(busy);
        replaceDirectorySelectedButton.setDisable(busy);
        replaceProgressIndicator.setVisible(busy);
        replaceProgressIndicator.setManaged(busy);
        replaceProgressLabel.setVisible(busy);
        replaceProgressLabel.setManaged(busy);
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasAlphabet = countRules(alphabetContent) > 0;
        boolean hasDirectory = replaceDirectoryField.getText() != null && !replaceDirectoryField.getText().isBlank();
        boolean hasBackupPath = backupPathField.getText() != null && !backupPathField.getText().isBlank();
        boolean hasLogPath = logPathField.getText() != null && !logPathField.getText().isBlank();
        boolean hasContextLogPath = contextLogPathField.getText() != null && !contextLogPathField.getText().isBlank();
        boolean hasComparisonTargets = !comparisonTargets.isEmpty();

        startReplaceButton.setDisable(busy || !hasDirectory || !hasAlphabet);
        openAlphabetEditorButton.setDisable(busy);
        openBackupPathButton.setDisable(!hasBackupPath);
        openLogPathButton.setDisable(!hasLogPath);
        openContextLogPathButton.setDisable(!hasContextLogPath);
        openComparisonDialogButton.setDisable(busy || !hasComparisonTargets);
    }

    private void clearReplaceResultFields() {
        replaceFilesCountField.clear();
        replaceRulesCountField.clear();
        replaceTotalCountField.clear();
        backupPathField.clear();
        logPathField.clear();
        contextLogPathField.clear();
        comparisonTargets = List.of();
    }

    private void unbindReplaceProgressState() {
        replaceProgressIndicator.progressProperty().unbind();
        replaceProgressLabel.textProperty().unbind();
        replaceProgressLabel.setText(REPLACE_PROGRESS_DEFAULT_TEXT);
    }

    private String formatReplaceProgressMessage(int progressPercent) {
        int boundedPercent = Math.min(100, Math.max(0, progressPercent));
        return REPLACE_PROGRESS_TEXT_TEMPLATE.formatted(boundedPercent);
    }

    private void updateAlphabetSummary() {
        int rulesCount = countRules(alphabetContent);
        alphabetSummaryField.setText(rulesCount > 0
                ? "Заполнено правил: " + rulesCount
                : "Алфавит еще не заполнен");
    }

    private void showAlphabetEditorDialog() {
        Window owner = replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null;

        Stage dialog = new Stage();
        dialog.setTitle("Редактор алфавита замен");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ObservableList<ReplacementRow> rows = parseAlphabetToRows(alphabetContent);
        if (rows.isEmpty()) {
            rows.add(new ReplacementRow("", ""));
        }

        TableView<ReplacementRow> table = createReplacementTable(rows);

        Button addRowButton = new Button("Добавить строку");
        Button deleteRowButton = new Button("Удалить строку");
        Button importButton = new Button("Импорт");
        Button saveButton = new Button("Сохранить");
        Button shareButton = new Button("Поделиться");
        Button applyButton = new Button("Применить");
        Button cancelButton = new Button("Отмена");

        deleteRowButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        applyButton.getStyleClass().add("alphabet-editor-apply-button");
        applyButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        addRowButton.setOnAction(event -> addEmptyRowAndEdit(table));

        deleteRowButton.setOnAction(event -> {
            int index = table.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                table.getItems().remove(index);
            }
            if (table.getItems().isEmpty()) {
                addEmptyRowAndEdit(table);
            }
        });

        importButton.setOnAction(event -> importAlphabetIntoTable(dialog, table));
        saveButton.setOnAction(event -> saveAlphabetFromTable(dialog, table));
        shareButton.setOnAction(event -> shareAlphabetFromTable(table));

        applyButton.setOnAction(event -> applyAlphabetFromTable(dialog, table));
        cancelButton.setOnAction(event -> dialog.close());

        HBox rowActions = new HBox(8, addRowButton, deleteRowButton, importButton, saveButton, shareButton);
        rowActions.getStyleClass().add("alphabet-editor-actions");
        HBox footer = new HBox(8, cancelButton, applyButton);
        footer.getStyleClass().add("alphabet-editor-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label hintLabel = new Label("Заполняйте пары: слева что менять, справа на что менять. Пустая строка подсвечивается как новая.");
        hintLabel.getStyleClass().add("section-label");

        VBox root = new VBox(10, hintLabel, rowActions, table, footer);
        root.getStyleClass().addAll("app-root", "alphabet-editor-root");
        root.setPadding(new Insets(14));

        Scene scene = new Scene(root, 860, 560);
        Scene ownerScene = replaceDirectoryField.getScene();
        if (ownerScene != null) {
            scene.getStylesheets().addAll(ownerScene.getStylesheets());
            inheritTheme(ownerScene, root);
        }

        dialog.setScene(scene);
        Platform.runLater(() -> focusFirstEditableRow(table));
        dialog.showAndWait();
    }

    private void showComparisonDialog() {
        Window owner = replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null;

        Stage dialog = new Stage();
        dialog.setTitle("Сравнение файлов Word");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        TableView<ComparisonTarget> table = createComparisonTable(comparisonTargets);

        Button closeButton = new Button("Закрыть");
        closeButton.setCancelButton(true);
        closeButton.setOnAction(event -> dialog.close());

        HBox footer = new HBox(8, closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, table, footer);
        root.getStyleClass().addAll("app-root", "word-comparison-root");
        root.setPadding(new Insets(14));

        Scene scene = new Scene(root, 760, 420);
        Scene ownerScene = replaceDirectoryField.getScene();
        if (ownerScene != null) {
            scene.getStylesheets().addAll(ownerScene.getStylesheets());
            inheritTheme(ownerScene, root);
        }

        dialog.setScene(scene);
        dialog.show();
    }

    private List<ComparisonTarget> buildComparisonTargets(AlphabetReplaceService.ReplaceOperationResult result) {
        if (result.changedFiles() == null || result.changedFiles().isEmpty()) {
            return List.of();
        }

        Set<Path> changedFiles = result.changedFiles().stream()
                .map(path -> path.toAbsolutePath().normalize())
                .collect(Collectors.toSet());

        return wordComparisonService.findComparisonTargets(result.sourcePath(), result.backupPath()).stream()
                .filter(target -> changedFiles.contains(target.revisedFile().toAbsolutePath().normalize()))
                .toList();
    }

    private TableView<ComparisonTarget> createComparisonTable(List<ComparisonTarget> comparisonTargets) {
        TableView<ComparisonTarget> table = new TableView<>(FXCollections.observableArrayList(comparisonTargets));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("word-comparison-table");

        TableColumn<ComparisonTarget, String> fileColumn = new TableColumn<>("Имя файла");
        fileColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().fileName()));
        fileColumn.setResizable(false);
        fileColumn.prefWidthProperty().bind(table.widthProperty().subtract(2).multiply(0.66));

        TableColumn<ComparisonTarget, ComparisonTarget> compareColumn = new TableColumn<>("Сравнение");
        compareColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        compareColumn.setCellFactory(column -> createComparisonLinkCell());
        compareColumn.setResizable(false);
        compareColumn.prefWidthProperty().bind(table.widthProperty().subtract(2).multiply(0.34));

        table.getColumns().setAll(fileColumn, compareColumn);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private TableCell<ComparisonTarget, ComparisonTarget> createComparisonLinkCell() {
        return new TableCell<>() {
            private final Hyperlink link = new Hyperlink("Сравнить");

            {
                link.setOnAction(event -> {
                    ComparisonTarget target = getItem();
                    if (target != null) {
                        openWordComparison(target);
                    }
                });
            }

            @Override
            protected void updateItem(ComparisonTarget item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setGraphic(link);
            }
        };
    }

    private void openWordComparison(ComparisonTarget target) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                wordComparisonService.openComparison(target.originalFile(), target.revisedFile());
                return null;
            }
        };

        task.setOnFailed(event -> showError("сравнение файлов Word", task.getException()));

        Thread worker = new Thread(task, "word-comparison");
        worker.setDaemon(true);
        worker.start();
    }

    private TableView<ReplacementRow> createReplacementTable(ObservableList<ReplacementRow> rows) {
        TableView<ReplacementRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("alphabet-editor-table");
        table.setRowFactory(ignored -> new TableRow<>() {
            private ReplacementRow boundItem;

            @Override
            protected void updateItem(ReplacementRow item, boolean empty) {
                if (boundItem != null) {
                    boundItem.fromProperty().removeListener(rowHeightListener);
                    boundItem.toProperty().removeListener(rowHeightListener);
                }
                super.updateItem(item, empty);
                pseudoClassStateChanged(NEW_ROW_PSEUDO_CLASS, !empty && item != null && item.isBlank());
                boundItem = empty ? null : item;
                if (boundItem == null) {
                    setPrefHeight(USE_COMPUTED_SIZE);
                    return;
                }

                boundItem.fromProperty().addListener(rowHeightListener);
                boundItem.toProperty().addListener(rowHeightListener);
                updatePreferredRowHeight(this, table, boundItem);
            }

            private final javafx.beans.InvalidationListener rowHeightListener =
                    observable -> updatePreferredRowHeight(this, table, boundItem);
        });

        TableColumn<ReplacementRow, String> fromColumn = new TableColumn<>("Что менять");
        fromColumn.setCellValueFactory(cellData -> cellData.getValue().fromProperty());
        fromColumn.setCellFactory(column -> createAutoCommitCell());
        fromColumn.setOnEditCommit(event -> event.getRowValue().setFrom(event.getNewValue()));
        fromColumn.setResizable(false);
        fromColumn.prefWidthProperty().bind(table.widthProperty().subtract(2).divide(2));

        TableColumn<ReplacementRow, String> toColumn = new TableColumn<>("На что менять");
        toColumn.setCellValueFactory(cellData -> cellData.getValue().toProperty());
        toColumn.setCellFactory(column -> createAutoCommitCell());
        toColumn.setOnEditCommit(event -> event.getRowValue().setTo(event.getNewValue()));
        toColumn.setResizable(false);
        toColumn.prefWidthProperty().bind(table.widthProperty().subtract(2).divide(2));

        table.getColumns().setAll(fromColumn, toColumn);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private void importAlphabetIntoTable(Stage dialog, TableView<ReplacementRow> table) {
        Path selectedFile = alphabetReplaceSectionHandler.selectAlphabetImportFile(dialog);
        if (selectedFile == null) {
            return;
        }

        try {
            String importedContent = alphabetReplaceSectionHandler.importAlphabet(selectedFile);
            ObservableList<ReplacementRow> importedRows = parseAlphabetToRows(importedContent);
            if (importedRows.isEmpty()) {
                importedRows.add(new ReplacementRow("", ""));
            }
            table.getItems().setAll(importedRows);
            outputArea.setText("Алфавит импортирован в редактор из файла: " + selectedFile.toAbsolutePath());
        } catch (Exception exception) {
            showError("импорт алфавита", exception);
        }
    }

    private void saveAlphabetFromTable(Stage dialog, TableView<ReplacementRow> table) {
        commitActiveCellEdit(table);
        String tableContent = serializeRows(table.getItems());
        if (tableContent.isBlank()) {
            showWarning(dialog, "Нет корректных правил для сохранения.");
            return;
        }

        Path selectedFile = alphabetReplaceSectionHandler.selectAlphabetExportFile(dialog);
        if (selectedFile == null) {
            return;
        }

        try {
            Path savedPath = alphabetReplaceSectionHandler.saveAlphabet(selectedFile, tableContent);
            outputArea.setText("Алфавит сохранен в файл: " + savedPath.toAbsolutePath());
        } catch (Exception exception) {
            showError("сохранение алфавита", exception);
        }
    }

    private void shareAlphabetFromTable(TableView<ReplacementRow> table) {
        commitActiveCellEdit(table);
        String tableContent = serializeRows(table.getItems());
        if (tableContent.isBlank()) {
            showWarning(replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null,
                    "Нет корректных правил для отправки.");
            return;
        }

        try {
            alphabetReplaceSectionHandler.copyAlphabetToClipboard(tableContent);
            outputArea.setText("Алфавит скопирован в буфер обмена. Теперь его можно отправить другому пользователю.");
        } catch (Exception exception) {
            showError("копирование алфавита", exception);
        }
    }

    private void applyAlphabetFromTable(Stage dialog, TableView<ReplacementRow> table) {
        commitActiveCellEdit(table);
        String tableContent = serializeRows(table.getItems());
        if (tableContent.isBlank()) {
            showWarning(dialog, "Добавьте хотя бы одно корректное правило: слева и справа должен быть текст.");
            return;
        }

        alphabetContent = tableContent;
        updateAlphabetSummary();
        updateActionButtons();
        outputArea.setText("Алфавит обновлен через редактор: " + countRules(alphabetContent) + " правил.");
        dialog.close();
    }

    private ObservableList<ReplacementRow> parseAlphabetToRows(String content) {
        ObservableList<ReplacementRow> rows = FXCollections.observableArrayList();
        if (content == null || content.isBlank()) {
            return rows;
        }

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            int arrowIndex = trimmed.indexOf("->");
            if (arrowIndex < 0) {
                continue;
            }

            String from = decodeAlphabetCell(trimmed.substring(0, arrowIndex).trim());
            String to = decodeAlphabetCell(trimmed.substring(arrowIndex + 2).trim());
            if (!from.isEmpty() && !to.isEmpty()) {
                rows.add(new ReplacementRow(from, to));
            }
        }

        return rows;
    }

    private String serializeRows(ObservableList<ReplacementRow> rows) {
        return rows.stream()
                .map(row -> new String[]{safeTrim(row.getFrom()), safeTrim(row.getTo())})
                .filter(parts -> !parts[0].isEmpty() && !parts[1].isEmpty())
                .map(parts -> encodeAlphabetCell(parts[0]) + " -> " + encodeAlphabetCell(parts[1]))
                .collect(Collectors.joining("\n"));
    }

    private int countRules(String content) {
        return parseAlphabetToRows(content).size();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String encodeAlphabetCell(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n");
    }

    private String decodeAlphabetCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder decoded = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaping) {
                decoded.append(current == 'n' ? '\n' : current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                decoded.append(current);
            }
        }
        if (escaping) {
            decoded.append('\\');
        }
        return decoded.toString();
    }

    private void showWarning(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Внимание");
        alert.setHeaderText("Недостаточно данных");
        alert.setContentText(message);
        alert.showAndWait();
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

        outputArea.setText("Ошибка: " + message);
    }

    private void inheritTheme(Scene ownerScene, VBox dialogRoot) {
        Parent ownerRoot = ownerScene.getRoot();
        if (ownerRoot == null) {
            return;
        }

        if (ownerRoot.getStyleClass().contains(LIGHT_THEME_CLASS)) {
            dialogRoot.getStyleClass().add(LIGHT_THEME_CLASS);
        } else if (ownerRoot.getStyleClass().contains(DARK_THEME_CLASS)) {
            dialogRoot.getStyleClass().add(DARK_THEME_CLASS);
        }
    }

    private void addEmptyRowAndEdit(TableView<ReplacementRow> table) {
        table.getItems().add(new ReplacementRow("", ""));
        int lastIndex = table.getItems().size() - 1;
        table.getSelectionModel().select(lastIndex);
        table.scrollTo(lastIndex);
        table.edit(lastIndex, table.getColumns().get(0));
    }

    private void focusFirstEditableRow(TableView<ReplacementRow> table) {
        if (table.getItems().isEmpty()) {
            addEmptyRowAndEdit(table);
            return;
        }

        int firstEmptyIndex = table.getItems().size() - 1;
        for (int i = 0; i < table.getItems().size(); i++) {
            if (table.getItems().get(i).isBlank()) {
                firstEmptyIndex = i;
                break;
            }
        }

        table.getSelectionModel().select(firstEmptyIndex);
        table.scrollTo(firstEmptyIndex);
        table.edit(firstEmptyIndex, table.getColumns().get(0));
    }

    private void commitActiveCellEdit(TableView<ReplacementRow> table) {
        if (table.getScene() != null) {
            table.requestFocus();
        }
    }

    private void updatePreferredRowHeight(TableRow<ReplacementRow> row, TableView<ReplacementRow> table, ReplacementRow item) {
        if (row == null || item == null) {
            return;
        }
        row.setPrefHeight(calculateRowHeight(table, item.getFrom(), item.getTo()));
    }

    private void updatePreferredRowHeight(TableRow<ReplacementRow> row, TableView<ReplacementRow> table, String from, String to) {
        if (row == null) {
            return;
        }
        row.setPrefHeight(calculateRowHeight(table, from, to));
    }

    private double calculateRowHeight(TableView<ReplacementRow> table, String from, String to) {
        double tableWidth = table == null ? 0 : table.getWidth();
        double textWidth = Math.max(80.0, (tableWidth - 2.0) / 2.0 - ALPHABET_EDITOR_CELL_HORIZONTAL_PADDING);
        int charactersPerLine = Math.max(8, (int) Math.floor(textWidth / ALPHABET_EDITOR_AVERAGE_CHAR_WIDTH));
        int lineCount = Math.max(countDisplayedLines(from, charactersPerLine), countDisplayedLines(to, charactersPerLine));
        return Math.max(ALPHABET_EDITOR_MIN_ROW_HEIGHT, lineCount * ALPHABET_EDITOR_LINE_HEIGHT + ALPHABET_EDITOR_ROW_VERTICAL_PADDING);
    }

    private int countDisplayedLines(String value, int charactersPerLine) {
        if (value == null || value.isEmpty()) {
            return 1;
        }

        int lines = 0;
        for (String paragraph : value.split("\\R", -1)) {
            lines += Math.max(1, (int) Math.ceil((double) paragraph.length() / charactersPerLine));
        }
        return lines;
    }

    private TableCell<ReplacementRow, String> createAutoCommitCell() {
        return new TableCell<>() {
            private final TextArea editor = new TextArea();

            {
                editor.getStyleClass().add("alphabet-editor-cell-field");
                editor.setWrapText(true);
                editor.setPrefRowCount(3);
                editor.textProperty().addListener((observable, oldValue, newValue) -> updateEditingRowHeight(newValue));
                editor.focusedProperty().addListener((observable, oldValue, focusedNow) -> {
                    if (!focusedNow) {
                        commitEdit(editor.getText());
                    }
                });
                editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                        commitEdit(editor.getText());
                        event.consume();
                    }
                });
                setWrapText(true);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                setOnMouseClicked(event -> {
                    if (!isEmpty() && !isEditing() && event.getClickCount() == 1) {
                        getTableView().edit(getIndex(), getTableColumn());
                    }
                });
            }

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                    return;
                }

                super.startEdit();
                editor.setText(getItem());
                setText(null);
                setGraphic(editor);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                Platform.runLater(() -> {
                    editor.requestFocus();
                    editor.selectAll();
                    updateEditingRowHeight(editor.getText());
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            public void commitEdit(String newValue) {
                String committedValue = newValue == null ? "" : newValue;
                if (!isEditing() && !committedValue.equals(getItem())) {
                    TableView<ReplacementRow> table = getTableView();
                    TableColumn<ReplacementRow, String> column = getTableColumn();
                    if (table != null && column != null) {
                        TableColumn.CellEditEvent<ReplacementRow, String> editEvent = new TableColumn.CellEditEvent<>(
                                table,
                                new TablePosition<>(table, getIndex(), column),
                                TableColumn.editCommitEvent(),
                                committedValue
                        );
                        Event.fireEvent(column, editEvent);
                    }
                }
                super.commitEdit(committedValue);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (isEditing()) {
                    editor.setText(item);
                    setText(null);
                    setGraphic(editor);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(item);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
            }

            private void updateEditingRowHeight(String editorText) {
                ReplacementRow rowItem = getTableRow() == null ? null : getTableRow().getItem();
                TableView<ReplacementRow> table = getTableView();
                TableColumn<ReplacementRow, String> column = getTableColumn();
                if (rowItem == null || table == null || column == null) {
                    return;
                }

                int columnIndex = table.getColumns().indexOf(column);
                String from = columnIndex == 0 ? editorText : rowItem.getFrom();
                String to = columnIndex == 1 ? editorText : rowItem.getTo();
                updatePreferredRowHeight(getTableRow(), table, from, to);
            }
        };
    }

    private static final class ReplacementRow {
        private final StringProperty from = new SimpleStringProperty("");
        private final StringProperty to = new SimpleStringProperty("");

        private ReplacementRow(String from, String to) {
            setFrom(from);
            setTo(to);
        }

        public StringProperty fromProperty() {
            return from;
        }

        public StringProperty toProperty() {
            return to;
        }

        public String getFrom() {
            return from.get();
        }

        public void setFrom(String value) {
            from.set(value == null ? "" : value);
        }

        public String getTo() {
            return to.get();
        }

        public void setTo(String value) {
            to.set(value == null ? "" : value);
        }

        public boolean isBlank() {
            return (getFrom() == null || getFrom().isBlank()) && (getTo() == null || getTo().isBlank());
        }
    }
}
