package com.desktop.document.turnover.service.api.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;

import java.nio.file.Path;
import java.util.List;

public interface ConverterService {

    int getFilesCountByType(Path directory, TypeFromDocs sourceType);

    int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType);

    List<TypeFromDocs> getTypes();
}
