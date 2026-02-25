package com.desktop.document.turnover.configuration;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConverterStrategyBeanTest {

    @Test
    void converterServiceMapBuildsMappingForAllDeclaredTypes() {
        ConverterService word = new StubConverterService(List.of(TypeFromDocs.DOC, TypeFromDocs.DOCX));
        ConverterService excel = new StubConverterService(List.of(TypeFromDocs.EXCEL));

        ConverterStrategyBean bean = new ConverterStrategyBean(List.of(word, excel));

        Map<TypeFromDocs, ConverterService> result = bean.converterServiceMap();

        assertEquals(3, result.size());
        assertSame(word, result.get(TypeFromDocs.DOC));
        assertSame(word, result.get(TypeFromDocs.DOCX));
        assertSame(excel, result.get(TypeFromDocs.EXCEL));
    }

    @Test
    void converterServiceMapUsesLastServiceForDuplicateType() {
        ConverterService first = new StubConverterService(List.of(TypeFromDocs.DOCX));
        ConverterService second = new StubConverterService(List.of(TypeFromDocs.DOCX));

        ConverterStrategyBean bean = new ConverterStrategyBean(List.of(first, second));

        Map<TypeFromDocs, ConverterService> result = bean.converterServiceMap();

        assertSame(second, result.get(TypeFromDocs.DOCX));
    }

    private record StubConverterService(List<TypeFromDocs> types) implements ConverterService {
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
            return types;
        }
    }
}
