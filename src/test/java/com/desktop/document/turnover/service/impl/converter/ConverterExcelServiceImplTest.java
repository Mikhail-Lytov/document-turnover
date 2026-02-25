package com.desktop.document.turnover.service.impl.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterExcelServiceImplTest {

    private final ConverterExcelServiceImpl service = new ConverterExcelServiceImpl();

    @Test
    void getFilesCountByTypeReturnsZeroForInvalidInput(@TempDir Path tempDir) {
        assertEquals(0, service.getFilesCountByType(null, TypeFromDocs.EXCEL));
        assertEquals(0, service.getFilesCountByType(tempDir, null));
        assertEquals(0, service.getFilesCountByType(tempDir.resolve("missing"), TypeFromDocs.EXCEL));
    }

    @Test
    void getFilesCountByTypeCountsMatchingExcelFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.xlsx"), "content");
        Files.writeString(tempDir.resolve("b.XLSX"), "content");
        Files.writeString(tempDir.resolve("c.docx"), "content");

        assertEquals(2, service.getFilesCountByType(tempDir, TypeFromDocs.EXCEL));
    }

    @Test
    void convertDirectoryReturnsZeroWhenNoMatchingFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.docx"), "content");

        assertEquals(0, service.convertDirectory(tempDir, TypeFromDocs.EXCEL, TypeToDocs.PDF));
    }

    @Test
    void convertDirectoryThrowsWhenArgumentsAreInvalid(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(null, TypeFromDocs.EXCEL, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(tempDir, null, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(tempDir, TypeFromDocs.EXCEL, null));
    }

    @Test
    void getTypesReturnsOnlyExcelType() {
        assertEquals(List.of(TypeFromDocs.EXCEL), service.getTypes());
    }
}
