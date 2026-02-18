package com.descktop.project.documentturnover.domain.enums;

import lombok.Getter;

public enum ResourcesSystemType {

    FILE("Файл"),
    DIRECTORY("Директория");

    @Getter
    private final String name;
    ResourcesSystemType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
