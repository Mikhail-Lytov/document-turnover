package com.desktop.document.turnover.controller.section;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConverterViewController {

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
    private TextField allFilesInformationParser;

    @FXML
    private TextField goodConverterInformationParser;

    @FXML
    private TextField oldVersionPathInformationParser;

    @FXML
    private TextField newVersionPathInformationParser;

    @FXML
    private void initialize() {
        converterSectionHandler.init(typeFrom, typeTo, startConverterButton, tfDirectory);
    }

    @FXML
    protected void directorySelectedButtonClick() {
        Window owner = tfDirectory.getScene() != null ? tfDirectory.getScene().getWindow() : null;
        converterSectionHandler.handleDirectorySelection(tfDirectory, startConverterButton, owner);
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
}
