package com.descktop.project.documentturnover;

import com.jacob.com.LibraryLoader;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, "C:\\libs\\jacob\\jacob-1.18-x64.dll");// TODO: Сделать динаический поиск
        Application.launch(HelloApplication.class, args);
    }
}
