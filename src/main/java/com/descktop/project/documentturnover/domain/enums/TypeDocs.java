package com.descktop.project.documentturnover.domain.enums;

import lombok.Getter;

public enum TypeDocs {

    DOCX("docx", 16),

    PDF("pdf", 17);

    @Getter
    private final String name;

    @Getter
    private final int wordFormat;
    TypeDocs(String name, int wordFormat) {
        this.name = name;
        this.wordFormat = wordFormat;
    }

    @Override
    public String toString() {
        return name;
    }
}
