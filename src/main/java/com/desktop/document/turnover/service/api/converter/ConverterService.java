package com.desktop.document.turnover.service.api.converter;

import com.desktop.document.turnover.domain.enums.TypeDocs;

import java.nio.file.Path;

public interface ConverterService {

    int getFilesCountByType(Path directory, TypeDocs sourceType);

    int convertDirectory(Path directory, TypeDocs sourceType, TypeDocs targetType);
}
