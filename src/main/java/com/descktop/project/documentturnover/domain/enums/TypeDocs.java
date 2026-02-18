package com.descktop.project.documentturnover.domain.enums;

import lombok.Getter;

public enum TypeDocs {

    DOCX("docx"),

    PDF("pdf");

    @Getter
    private final String name;
    TypeDocs(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
