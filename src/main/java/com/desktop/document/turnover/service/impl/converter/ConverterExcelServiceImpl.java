package com.desktop.document.turnover.service.impl.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConverterExcelServiceImpl implements ConverterService {
    @Override
    public int getFilesCountByType(Path directory, TypeFromDocs sourceType) {
        return 0;
    }

    @Override
    public int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType) {
        return 0;
    }

    @Override
    public List<TypeFromDocs> getTypes() {
        return List.of(
                TypeFromDocs.EXCEL
        );
    }
}
