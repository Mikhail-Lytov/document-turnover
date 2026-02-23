package com.desktop.document.turnover.domain.enums;

import lombok.Getter;

public enum TypeFromDocs {

    DOCX("docx", 16),

   // PDF("pdf", 17),

    DOC("doc", 0),

    DOS("HTML", 10 ),

    EXCEL("XLSX", 61);

    @Getter
    private final String name;

    @Getter
    private final int wordFormat;
    TypeFromDocs(String name, int wordFormat) {
        this.name = name;
        this.wordFormat = wordFormat;
    }

    @Override
    public String toString() {
        return name;
    }
}
