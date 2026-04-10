package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlphabetReplaceViewController {

    private final AlphabetReplaceSectionHandler alphabetReplaceSectionHandler;

    @FXML
    private TextField replaceDirectoryField;

    @FXML
    private TextField alphabetSummaryField;

    @FXML
    private Button openAlphabetEditorButton;

    @FXML
    private Button startReplaceButton;

    @FXML
    private TextField replaceFilesCountField;

    @FXML
    private TextField replaceRulesCountField;

    @FXML
    private TextField replaceTotalCountField;

    @FXML
    private TextField backupPathField;

    @FXML
    private TextField logPathField;

    @FXML
    private TextField contextLogPathField;

    @FXML
    private TextArea outputArea;

    private boolean busy;
    private String alphabetContent = "";

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
        runTask(
                () -> alphabetReplaceSectionHandler.replace(replaceDirectoryField.getText(), alphabetContent),
                this::applyReplaceResult,
                "замену"
        );
    }

    private <T> void runTask(Callable<T> action, Consumer<T> onSuccess, String actionName) {
        setBusy(true);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError(actionName, task.getException());
        });

        Thread worker = new Thread(task, "alphabet-replace-" + actionName);
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
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasAlphabet = countRules(alphabetContent) > 0;
        boolean hasDirectory = replaceDirectoryField.getText() != null && !replaceDirectoryField.getText().isBlank();

        startReplaceButton.setDisable(busy || !hasDirectory || !hasAlphabet);
        openAlphabetEditorButton.setDisable(busy);
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
        applyButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        addRowButton.setOnAction(event -> {
            table.getItems().add(new ReplacementRow("", ""));
            int lastIndex = table.getItems().size() - 1;
            table.getSelectionModel().select(lastIndex);
            table.scrollTo(lastIndex);
        });

        deleteRowButton.setOnAction(event -> {
            int index = table.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                table.getItems().remove(index);
            }
            if (table.getItems().isEmpty()) {
                table.getItems().add(new ReplacementRow("", ""));
            }
        });

        importButton.setOnAction(event -> importAlphabetIntoTable(dialog, table));
        saveButton.setOnAction(event -> saveAlphabetFromTable(dialog, table));
        shareButton.setOnAction(event -> shareAlphabetFromTable(table));

        applyButton.setOnAction(event -> applyAlphabetFromTable(dialog, table));
        cancelButton.setOnAction(event -> dialog.close());

        HBox rowActions = new HBox(8, addRowButton, deleteRowButton, importButton, saveButton, shareButton);
        HBox footer = new HBox(8, cancelButton, applyButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label hintLabel = new Label("Заполняйте строки таблицы: слева что менять, справа на что менять.");

        VBox root = new VBox(10, hintLabel, rowActions, table, footer);
        root.setPadding(new Insets(14));

        Scene scene = new Scene(root, 860, 560);
        Scene ownerScene = replaceDirectoryField.getScene();
        if (ownerScene != null) {
            scene.getStylesheets().addAll(ownerScene.getStylesheets());
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private TableView<ReplacementRow> createReplacementTable(ObservableList<ReplacementRow> rows) {
        TableView<ReplacementRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<ReplacementRow, String> fromColumn = new TableColumn<>("Что менять");
        fromColumn.setCellValueFactory(cellData -> cellData.getValue().fromProperty());
        fromColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        fromColumn.setOnEditCommit(event -> event.getRowValue().setFrom(event.getNewValue()));

        TableColumn<ReplacementRow, String> toColumn = new TableColumn<>("На что менять");
        toColumn.setCellValueFactory(cellData -> cellData.getValue().toProperty());
        toColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        toColumn.setOnEditCommit(event -> event.getRowValue().setTo(event.getNewValue()));

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

            String from = trimmed.substring(0, arrowIndex).trim();
            String to = trimmed.substring(arrowIndex + 2).trim();
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
                .map(parts -> parts[0] + " -> " + parts[1])
                .collect(Collectors.joining("\n"));
    }

    private int countRules(String content) {
        return parseAlphabetToRows(content).size();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
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
    }
}
