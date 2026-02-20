package com.descktop.project.documentturnover.controller;

import com.descktop.project.documentturnover.controller.section.AlphabetReplaceSectionHandler;
import com.descktop.project.documentturnover.controller.section.ConverterSectionHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainView {

    private final ConverterSectionHandler converterSectionHandler;
    private final AlphabetReplaceSectionHandler alphabetReplaceSectionHandler;

    @FXML
    private void initialize() {
        converterSectionHandler.init(typeFrom, typeTo, startConverterButton, tfDirectory);
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

    @FXML
    protected Button startConverterButton;

    @FXML
    protected void directorySelectedButtonClick() {
        Window owner = tfDirectory.getScene() != null ? tfDirectory.getScene().getWindow() : null;
        converterSectionHandler.handleDirectorySelection(tfDirectory, startConverterButton, owner);
    }

    @FXML
    protected void converterButtonClick() {
        converterSectionHandler.show(convertBox, vBoxInformationParser);
    }

    @FXML
    protected void startConverterButtonClick() {
        converterSectionHandler.convert(
                tfDirectory,
                allFilesInformationParser,
                goodConverterInformationParser,
                oldVersionPathInformationParser,
                newVersionPathInformationParser
        );
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
        alphabetReplaceSectionHandler.show(convertBox, vBoxInformationParser);
    }
}
