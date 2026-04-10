package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AlphabetReplaceViewController {

    private final AlphabetReplaceSectionHandler alphabetReplaceSectionHandler;

    @FXML
    private TextField replaceDirectoryField;

    @FXML
    private TextField alphabetFileField;

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

    @FXML
    private void initialize() {
        alphabetReplaceSectionHandler.initReplace(replaceDirectoryField, alphabetFileField, startReplaceButton);
        outputArea.setText("Готово к работе. Выберите папку, файл алфавита и запустите замену.");
    }

    @FXML
    protected void replaceDirectorySelectedButtonClick() {
        Window owner = replaceDirectoryField.getScene() != null ? replaceDirectoryField.getScene().getWindow() : null;
        alphabetReplaceSectionHandler.selectDirectory(replaceDirectoryField, owner);
    }

    @FXML
    protected void alphabetFileSelectedButtonClick() {
        Window owner = alphabetFileField.getScene() != null ? alphabetFileField.getScene().getWindow() : null;
        alphabetReplaceSectionHandler.selectAlphabetFile(alphabetFileField, owner);
    }

    @FXML
    protected void startReplaceButtonClick() {
        runTask(
                () -> alphabetReplaceSectionHandler.replace(replaceDirectoryField.getText(), alphabetFileField.getText()),
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
        startReplaceButton.setDisable(busy || replaceDirectoryField.getText().isBlank() || alphabetFileField.getText().isBlank());
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
}
