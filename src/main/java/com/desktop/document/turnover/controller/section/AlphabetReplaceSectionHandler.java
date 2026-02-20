package com.desktop.document.turnover.controller.section;

import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class AlphabetReplaceSectionHandler {

    public void show(VBox convertBox, VBox informationBox) {
        convertBox.setVisible(false);
        informationBox.setVisible(false);
    }
}
