package com.desktop.document.turnover.service.api.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterServiceDefaultMethodsTest {

    private final ConverterService converterService = new TestConverterService();

    @Test
    void findFilesByExtensionFindsFilesRecursivelyAndCaseInsensitive(@TempDir Path tempDir) throws IOException {
        Path nested = Files.createDirectories(tempDir.resolve("nested"));

        Files.writeString(tempDir.resolve("first.DOCX"), "content");
        Files.writeString(nested.resolve("second.docx"), "content");
        Files.writeString(nested.resolve("ignore.txt"), "content");

        List<Path> results = converterService.findFilesByExtension(tempDir, "docx");

        assertEquals(2, results.size());
    }

    @Test
    void findFilesByExtensionThrowsUncheckedIoExceptionForMissingDirectory(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing");

        assertThrows(UncheckedIOException.class, () -> converterService.findFilesByExtension(missing, "docx"));
    }

    @Test
    void defaultValidateThrowsForNullArguments(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> converterService.defaultValidate(null, TypeFromDocs.DOCX, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> converterService.defaultValidate(tempDir, null, TypeToDocs.PDF));
        assertThrows(IllegalArgumentException.class, () -> converterService.defaultValidate(tempDir, TypeFromDocs.DOCX, null));
    }

    @Test
    void defaultValidateThrowsWhenPathIsNotDirectory(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("file.txt"), "text");

        assertThrows(IllegalArgumentException.class, () -> converterService.defaultValidate(file, TypeFromDocs.DOCX, TypeToDocs.PDF));
    }

    private static class TestConverterService implements ConverterService {
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
            return List.of();
        }
    }
}
