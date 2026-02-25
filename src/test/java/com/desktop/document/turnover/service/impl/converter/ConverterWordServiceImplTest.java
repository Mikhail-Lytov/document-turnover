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

class ConverterWordServiceImplTest {

    private final ConverterWordServiceImpl service = new ConverterWordServiceImpl();

    @Test
    void getFilesCountByTypeReturnsZeroForInvalidInput(@TempDir Path tempDir) {
        assertEquals(0, service.getFilesCountByType(null, TypeFromDocs.DOCX));
        assertEquals(0, service.getFilesCountByType(tempDir, null));
        assertEquals(0, service.getFilesCountByType(tempDir.resolve("missing"), TypeFromDocs.DOCX));
    }

    @Test
    void getFilesCountByTypeCountsMatchingFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.docx"), "content");
        Files.writeString(tempDir.resolve("b.DOCX"), "content");
        Files.writeString(tempDir.resolve("c.txt"), "content");

        assertEquals(2, service.getFilesCountByType(tempDir, TypeFromDocs.DOCX));
    }

    @Test
    void convertDirectoryReturnsZeroWhenNoMatchingFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "content");

        assertEquals(0, service.convertDirectory(tempDir, TypeFromDocs.DOCX, TypeToDocs.PDF));
    }

    @Test
    void convertDirectoryThrowsWhenArgumentsAreInvalid(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(null, TypeFromDocs.DOCX, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(tempDir, null, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> service.convertDirectory(tempDir, TypeFromDocs.DOCX, null));
    }

    @Test
    void getTypesReturnsWordSupportedTypesInOrder() {
        assertEquals(List.of(TypeFromDocs.DOS, TypeFromDocs.DOCX, TypeFromDocs.DOC, TypeFromDocs.RTF), service.getTypes());
    }
}
