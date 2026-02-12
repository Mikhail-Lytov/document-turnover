package com.descktop.project.documentturnover;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.descktop.project.documentturnover.service.ConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainView {

    private final ConverterService converterService;

    @FXML
    protected VBox convertBox;

    @FXML
    protected VBox vBoxInformationParser;
    @FXML
    protected TextField allFilesInformationParser;
    @FXML
    protected TextField goodConverterInformationParser;

    @FXML
    protected TextField oldVersionPathInformationParser;

    @FXML
    protected TextField newVersionPathInformationParser;

    @FXML
    protected void converterButtonClick() {
        convertBox.setVisible(true);
        vBoxInformationParser.setVisible(true);
        System.out.println("Converter button clicked! " + converterService.getClass().getSimpleName());
    }

    @FXML
    protected void searchByAlphabetButtonClick() {
        convertBox.setVisible(false);
        vBoxInformationParser.setVisible(false);
    }
}
