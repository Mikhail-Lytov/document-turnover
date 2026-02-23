package com.desktop.document.turnover.domain.enums;

import lombok.Getter;

public enum TypeToDocs {

    PDF("pdf", 17);

    @Getter
    private final String name;

    @Getter
    private final int wordFormat;
    TypeToDocs(String name, int wordFormat) {
        this.name = name;
        this.wordFormat = wordFormat;
    }

    @Override
    public String toString() {
        return name;
    }
}
