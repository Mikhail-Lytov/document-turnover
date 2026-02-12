package com.descktop.project.documentturnover;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class MainView {

    //MAIN VIEW COMPONENT
    @FXML
    protected VBox convertBox;


    //INFORMATION VIEW PARSER

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


    // MAIN VIEW BUTTON
    @FXML
    protected void converterButtonClick() {
        convertBox.setVisible(true);
        vBoxInformationParser.setVisible(true);
        System.out.println("Converter button clicked!");
    }

    @FXML
    protected void searchByAlphabetButtonClick() {
        convertBox.setVisible(false);
        vBoxInformationParser.setVisible(false);
    }
}
