package com.desktop.document.turnover.controller.section;

import com.desktop.document.turnover.domain.enums.ResourcesSystemType;
import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import com.desktop.document.turnover.service.impl.converter.ConverterStrategy;
import com.desktop.document.turnover.utils.GenerateMenuButton;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConverterSectionHandler {

    private final ConverterStrategy converterStrategy;
    private TypeFromDocs selectedTypeFrom;
    private TypeToDocs selectedTypeTo;

    public void init(MenuButton typeFrom, MenuButton typeTo, Button startConverterButton, TextField tfDirectory) {
        initTypeFrom(typeFrom, startConverterButton, tfDirectory);
        initTypeTo(typeTo, startConverterButton, tfDirectory);
        updateStartConverterButtonState(startConverterButton, tfDirectory);
    }

    public void show(VBox convertBox, VBox informationBox) {
        convertBox.setVisible(true);
        informationBox.setVisible(true);
    }

    public void handleDirectorySelection(TextField tfDirectory, Button startConverterButton, Window owner) {
        ChoiceDialog<ResourcesSystemType> choiceDialog = new ChoiceDialog<>(ResourcesSystemType.DIRECTORY, ResourcesSystemType.FILE);
        choiceDialog.setTitle("Выбор источника");
        choiceDialog.setHeaderText("Что хотите выбрать?");
        choiceDialog.setContentText("Тип:");

        Optional<ResourcesSystemType> selectedType = choiceDialog.showAndWait();
        if (selectedType.isEmpty()) {
            return;
        }

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
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        }
    }

    public void convert(
            TextField tfDirectory,
            TextField allFilesInformationParser,
            TextField goodConverterInformationParser,
            TextField oldVersionPathInformationParser,
            TextField newVersionPathInformationParser
    ) {
        if (selectedTypeFrom == null || selectedTypeTo == null || tfDirectory.getText().isBlank()) {
            return;
        }

        Path selectedDirectory = Path.of(tfDirectory.getText());
        ConverterService converterService = converterStrategy.getConverterService(selectedTypeFrom);

        int allFiles = converterService.getFilesCountByType(selectedDirectory, selectedTypeFrom);
        int convertedFiles = converterService.convertDirectory(selectedDirectory, selectedTypeFrom, selectedTypeTo);

        allFilesInformationParser.setText(String.valueOf(allFiles));
        goodConverterInformationParser.setText(String.valueOf(convertedFiles));
        oldVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
        newVersionPathInformationParser.setText(selectedDirectory.toAbsolutePath().toString());
    }

    private void initTypeFrom(MenuButton typeFrom, Button startConverterButton, TextField tfDirectory) {
        GenerateMenuButton.generateMenuButton(typeFrom, TypeFromDocs.class, selected -> {
            typeFrom.setText(selected.name());
            selectedTypeFrom = selected;
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        });
    }

    private void initTypeTo(MenuButton typeTo, Button startConverterButton, TextField tfDirectory) {
        GenerateMenuButton.generateMenuButton(typeTo, TypeToDocs.class, selected -> {
            typeTo.setText(selected.name());
            selectedTypeTo = selected;
            updateStartConverterButtonState(startConverterButton, tfDirectory);
        });
    }

    private void updateStartConverterButtonState(Button startConverterButton, TextField tfDirectory) {
        if (startConverterButton == null) {
            return;
        }

        boolean directoryIsBlank = tfDirectory == null || tfDirectory.getText().isBlank();
        startConverterButton.setDisable(selectedTypeFrom == null || selectedTypeTo == null || directoryIsBlank);
    }
}
