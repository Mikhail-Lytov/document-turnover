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
public class SearchDocumentsViewController {

    private final AlphabetReplaceSectionHandler alphabetReplaceSectionHandler;

    @FXML
    private TextField searchDirectoryField;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button startSearchButton;

    @FXML
    private TextField searchFilesCountField;

    @FXML
    private TextField searchMatchesCountField;

    @FXML
    private TextArea outputArea;

    @FXML
    private void initialize() {
        alphabetReplaceSectionHandler.initSearch(searchDirectoryField, searchTextField, startSearchButton);
        outputArea.setText("Готово к работе. Выберите папку и запустите поиск.");
    }

    @FXML
    protected void searchDirectorySelectedButtonClick() {
        Window owner = searchDirectoryField.getScene() == null ? null : searchDirectoryField.getScene().getWindow();
        alphabetReplaceSectionHandler.selectDirectory(searchDirectoryField, owner);
    }

    @FXML
    protected void startSearchButtonClick() {
        runTask(
                () -> alphabetReplaceSectionHandler.search(searchDirectoryField.getText(), searchTextField.getText()),
                this::applySearchResult,
                "поиск"
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

        Thread worker = new Thread(task, "search-documents-" + actionName);
        worker.setDaemon(true);
        worker.start();
    }

    private void applySearchResult(AlphabetReplaceService.SearchOperationResult result) {
        searchFilesCountField.setText(String.valueOf(result.filesScanned()));
        searchMatchesCountField.setText(String.valueOf(result.totalMatches()));
        outputArea.setText(result.report());
    }

    private void setBusy(boolean busy) {
        startSearchButton.setDisable(busy || searchDirectoryField.getText().isBlank() || searchTextField.getText().isBlank());
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
