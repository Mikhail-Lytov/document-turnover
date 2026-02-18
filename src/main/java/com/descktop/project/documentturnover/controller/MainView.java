package com.descktop.project.documentturnover.controller;

import com.descktop.project.documentturnover.domain.enums.ResourcesSystemType;
import com.descktop.project.documentturnover.domain.enums.TypeDocs;
import com.descktop.project.documentturnover.utils.GenerateMenuButton;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.descktop.project.documentturnover.service.ConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MainView {

    private final ConverterService converterService;

    @FXML
    private void initialize() {
        initTypeFrom();
        initTypeTo();
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
        });
    }

    private void initTypeTo() {
        GenerateMenuButton.generateMenuButton(typeTo, TypeDocs.class, selected -> {
            typeTo.setText(selected.name());
        });
    }


    //Кнопки конвертера

    /**
     * Кнопка выбора
     */
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
        }
    }

    @FXML
    protected void converterButtonClick() {
        convertBox.setVisible(true);
        vBoxInformationParser.setVisible(true);
        System.out.println("Converter button clicked! " + converterService.getClass().getSimpleName());
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
