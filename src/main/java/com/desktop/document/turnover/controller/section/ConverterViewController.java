package com.desktop.document.turnover.controller.section;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConverterViewController {

    private static final String CONVERSION_PROGRESS_DEFAULT_TEXT = "Идет конвертация...";
    private static final String CONVERSION_PROGRESS_TEXT_TEMPLATE = "Идет конвертация... %d%%";

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
    private Button directorySelectedButton;

    @FXML
    private ProgressIndicator converterProgressIndicator;

    @FXML
    private Label converterProgressLabel;

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

    private boolean startConverterButtonDisabledBeforeConversion;
    private boolean directorySelectedButtonDisabledBeforeConversion;
    private boolean typeFromDisabledBeforeConversion;
    private boolean typeToDisabledBeforeConversion;
    private boolean tfDirectoryDisabledBeforeConversion;

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
        clearConversionResultFields();
        updateOpenPathButtonsState();
        runConversionTask();
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

    private void runConversionTask() {
        setConversionInProgress(true);

        Task<ConverterSectionHandler.ConversionResult> task = new Task<>() {
            @Override
            protected ConverterSectionHandler.ConversionResult call() {
                updateProgress(0, 1);
                updateMessage(formatProgressMessage(0));

                ConverterSectionHandler.ConversionResult result = converterSectionHandler.convert(
                        tfDirectory.getText(),
                        (processedFiles, totalFiles) -> {
                            if (totalFiles <= 0) {
                                updateProgress(1, 1);
                                updateMessage(formatProgressMessage(100));
                                return;
                            }

                            int boundedProcessedFiles = Math.min(totalFiles, Math.max(0, processedFiles));
                            int progressPercent = (int) Math.round((boundedProcessedFiles * 100.0) / totalFiles);
                            updateProgress(boundedProcessedFiles, totalFiles);
                            updateMessage(formatProgressMessage(progressPercent));
                        }
                );

                updateProgress(1, 1);
                updateMessage(formatProgressMessage(100));
                return result;
            }
        };

        converterProgressIndicator.progressProperty().bind(task.progressProperty());
        converterProgressLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            unbindProgressState();
            setConversionInProgress(false);
            applyConversionResult(task.getValue());
        });

        task.setOnFailed(event -> {
            unbindProgressState();
            setConversionInProgress(false);
            showError("конвертацию", task.getException());
        });

        Thread worker = new Thread(task, "converter-conversion");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyConversionResult(ConverterSectionHandler.ConversionResult result) {
        allFilesInformationParser.setText(String.valueOf(result.allFiles()));
        goodConverterInformationParser.setText(String.valueOf(result.convertedFiles()));
        oldVersionPathInformationParser.setText(result.backupPath());
        newVersionPathInformationParser.setText(result.convertedPath());
        updateOpenPathButtonsState();
    }

    private void clearConversionResultFields() {
        allFilesInformationParser.clear();
        goodConverterInformationParser.clear();
        oldVersionPathInformationParser.clear();
        newVersionPathInformationParser.clear();
    }

    private void setConversionInProgress(boolean inProgress) {
        if (inProgress) {
            startConverterButtonDisabledBeforeConversion = startConverterButton.isDisable();
            directorySelectedButtonDisabledBeforeConversion = directorySelectedButton.isDisable();
            typeFromDisabledBeforeConversion = typeFrom.isDisable();
            typeToDisabledBeforeConversion = typeTo.isDisable();
            tfDirectoryDisabledBeforeConversion = tfDirectory.isDisable();
            converterProgressIndicator.setProgress(0);
            converterProgressLabel.setText(formatProgressMessage(0));
        }

        startConverterButton.setDisable(inProgress || startConverterButtonDisabledBeforeConversion);
        directorySelectedButton.setDisable(inProgress || directorySelectedButtonDisabledBeforeConversion);
        typeFrom.setDisable(inProgress || typeFromDisabledBeforeConversion);
        typeTo.setDisable(inProgress || typeToDisabledBeforeConversion);
        tfDirectory.setDisable(inProgress || tfDirectoryDisabledBeforeConversion);
        converterProgressIndicator.setVisible(inProgress);
        converterProgressIndicator.setManaged(inProgress);
        converterProgressLabel.setVisible(inProgress);
        converterProgressLabel.setManaged(inProgress);
    }

    private void unbindProgressState() {
        converterProgressIndicator.progressProperty().unbind();
        converterProgressLabel.textProperty().unbind();
        converterProgressLabel.setText(CONVERSION_PROGRESS_DEFAULT_TEXT);
    }

    private String formatProgressMessage(int progressPercent) {
        int boundedPercent = Math.min(100, Math.max(0, progressPercent));
        return CONVERSION_PROGRESS_TEXT_TEMPLATE.formatted(boundedPercent);
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
