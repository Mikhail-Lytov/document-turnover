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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        outputArea.setEditable(false);
        outputArea.setText("Готово к работе. Выберите папку и запустите поиск.");
    }

    @FXML
    protected void searchDirectorySelectedButtonClick() {
        Window owner = searchDirectoryField.getScene() == null ? null : searchDirectoryField.getScene().getWindow();
        alphabetReplaceSectionHandler.selectDirectory(searchDirectoryField, owner);
    }

    @FXML
    protected void startSearchButtonClick() {
        outputArea.setText("Поиск выполняется, подождите...");
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
        outputArea.setText(buildExecutionJournal(result));
    }

    private String buildExecutionJournal(AlphabetReplaceService.SearchOperationResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("=== ЖУРНАЛ ПОИСКА ===");
        lines.add("Проверено файлов: " + result.filesScanned());
        lines.add("Найдено совпадений: " + result.totalMatches());
        lines.add("Файлов с совпадениями: " + result.matches().size());

        if (result.matches().isEmpty()) {
            lines.add("");
            lines.add("Совпадения не найдены.");
        } else {
            List<AlphabetReplaceService.SearchFileResult> sortedMatches = result.matches().stream()
                    .sorted(Comparator.comparingInt(AlphabetReplaceService.SearchFileResult::matches).reversed()
                            .thenComparing(AlphabetReplaceService.SearchFileResult::fileName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            lines.add("");
            lines.add("Совпадения по файлам:");
            int index = 1;
            for (AlphabetReplaceService.SearchFileResult fileMatch : sortedMatches) {
                lines.add(index + ". " + fileMatch.fileName() + " — " + fileMatch.matches() + " " + formatMatchesWord(fileMatch.matches()));
                lines.add("   " + formatPagesInfo(fileMatch.pages(), fileMatch.matches()));
                index++;
            }
        }

        if (!result.errors().isEmpty()) {
            lines.add("");
            lines.add("Ошибки:");
            for (String error : result.errors()) {
                lines.add("  - " + error);
            }
        }

        return String.join("\n", lines);
    }

    private String formatPagesInfo(List<Integer> pages, int totalMatches) {
        if (pages == null || pages.isEmpty()) {
            return "Страницы: не удалось определить.";
        }

        Map<Integer, Integer> pageDistribution = new TreeMap<>();
        for (Integer page : pages) {
            if (page != null && page > 0) {
                pageDistribution.merge(page, 1, Integer::sum);
            }
        }

        if (pageDistribution.isEmpty()) {
            return "Страницы: не удалось определить.";
        }

        String pagesInfo = pageDistribution.entrySet().stream()
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));

        int unknownPagesMatches = Math.max(0, totalMatches - pages.size());
        if (unknownPagesMatches > 0) {
            return "Страницы: " + pagesInfo + "; без номера страницы: " + unknownPagesMatches + ".";
        }
        return "Страницы: " + pagesInfo + ".";
    }

    private String formatMatchesWord(int count) {
        int remainderHundred = count % 100;
        int remainderTen = count % 10;

        if (remainderHundred >= 11 && remainderHundred <= 14) {
            return "совпадений";
        }
        if (remainderTen == 1) {
            return "совпадение";
        }
        if (remainderTen >= 2 && remainderTen <= 4) {
            return "совпадения";
        }
        return "совпадений";
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
