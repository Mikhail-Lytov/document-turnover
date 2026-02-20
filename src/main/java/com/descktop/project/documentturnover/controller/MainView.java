package com.descktop.project.documentturnover.controller;

import com.descktop.project.documentturnover.domain.enums.ResourcesSystemType;
import com.descktop.project.documentturnover.domain.enums.TypeDocs;
import com.descktop.project.documentturnover.utils.GenerateMenuButton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.descktop.project.documentturnover.service.impl.converter.ConverterServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MainView {

    private final ConverterServiceImpl converterServiceImpl;
    private TypeDocs selectedTypeFrom;
    private TypeDocs selectedTypeTo;

    @FXML
    private void initialize() {
        initTypeFrom();
        initTypeTo();
        updateStartConverterButtonState();
    }

    // Окна конвертера
    @FXML
    protected VBox convertBox;

    @FXML
    protected VBox vBoxInformationParser;

    //Поля ввода данных в конвертации

    @FXML
    protected TextField tfDirectory;

    //Кнопки списка в конвертации

    @FXML
    protected MenuButton typeFrom;

    @FXML
    protected MenuButton typeTo;

    private void initTypeFrom() {
        GenerateMenuButton.generateMenuButton(typeFrom, TypeDocs.class, selected -> {
            typeFrom.setText(selected.name());
            selectedTypeFrom = selected;
            updateStartConverterButtonState();
        });
    }

    private void initTypeTo() {
        GenerateMenuButton.generateMenuButton(typeTo, TypeDocs.class, selected -> {
            typeTo.setText(selected.name());
            selectedTypeTo = selected;
            updateStartConverterButtonState();
        });
    }

    private void updateStartConverterButtonState() {
        if (startConverterButton != null) {
            startConverterButton.setDisable(selectedTypeFrom == null || selectedTypeTo == null || tfDirectory.getText().isEmpty());
        }
    }


    //Кнопки конвертера

    /**
     * Кнопка выбора
     */
    @FXML
    protected Button startConverterButton;

    @FXML
    protected void directorySelectedButtonClick() {
        ChoiceDialog<ResourcesSystemType> choiceDialog = new ChoiceDialog<ResourcesSystemType>(ResourcesSystemType.DIRECTORY, ResourcesSystemType.FILE);
        choiceDialog.setTitle("Выбор источника");
        choiceDialog.setHeaderText("Что хотите выбрать?");
        choiceDialog.setContentText("Тип:");

        Optional<ResourcesSystemType> selectedType = choiceDialog.showAndWait();
        if (selectedType.isEmpty()) {
            return;
        }

        Window owner = tfDirectory.getScene() != null ? tfDirectory.getScene().getWindow() : null;
        File selected = null;

        if (ResourcesSystemType.FILE.equals(selectedType.get())) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл");
            selected = fileChooser.showOpenDialog(owner);
        } else {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Выберите папку");
            selected = directoryChooser.showDialog(owner);
        }

        if (selected != null) {
            tfDirectory.setText(selected.getAbsolutePath());
            updateStartConverterButtonState();
        }
    }

    @FXML
    protected void converterButtonClick() {
        convertBox.setVisible(true);
        vBoxInformationParser.setVisible(true);
        System.out.println("Converter button clicked! " + converterServiceImpl.getClass().getSimpleName());
    }

    @FXML
    protected void startConverterButtonClick() {
        if (selectedTypeFrom == null || selectedTypeTo == null || tfDirectory.getText().isBlank()) {
            return;
        }

        Path selectedDirectory = Path.of(tfDirectory.getText());
        int allFiles = converterServiceImpl.getFilesCountByType(selectedDirectory, selectedTypeFrom);
        int convertedFiles = converterServiceImpl.convertDirectory(selectedDirectory, selectedTypeFrom, selectedTypeTo);

        allFilesInformationParser.setText(String.valueOf(allFiles));
        goodConverterInformationParser.setText(String.valueOf(convertedFiles));
        oldVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
        newVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
    }

    //
    @FXML
    protected TextField allFilesInformationParser;


    @FXML
    protected TextField goodConverterInformationParser;

    @FXML
    protected TextField oldVersionPathInformationParser;

    @FXML
    protected TextField newVersionPathInformationParser;

    @FXML
    protected void searchByAlphabetButtonClick() {
        convertBox.setVisible(false);
        vBoxInformationParser.setVisible(false);
    }
}
