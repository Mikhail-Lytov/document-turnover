package com.descktop.project.documentturnover.service.api.converter;

import com.descktop.project.documentturnover.domain.enums.TypeDocs;

import java.nio.file.Path;

public interface ConverterService {

    int getFilesCountByType(Path directory, TypeDocs sourceType);

    int convertDirectory(Path directory, TypeDocs sourceType, TypeDocs targetType);
}
