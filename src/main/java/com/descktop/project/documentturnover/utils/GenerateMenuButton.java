package com.descktop.project.documentturnover.utils;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import java.util.function.Consumer;

public class GenerateMenuButton {

    public static <E extends Enum<E>> void generateMenuButton(
            MenuButton menuButton,
            Class<E> enumClass,
            Consumer<E> onSelect
    ) {
        menuButton.getItems().clear();

        for (E value : enumClass.getEnumConstants()) {
            MenuItem item = new MenuItem(value.name());
            item.setOnAction(event -> {
                onSelect.accept(value);
            });
            menuButton.getItems().add(item);
        }
    }
}
