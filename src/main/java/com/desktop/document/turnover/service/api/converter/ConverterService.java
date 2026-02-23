package com.desktop.document.turnover.service.api.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ConverterService {

    int getFilesCountByType(Path directory, TypeFromDocs sourceType);

    int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType);

    List<TypeFromDocs> getTypes();

    default List<Path> findFilesByExtension(Path directory, String extension) {
        String normalizedExtension = "." + extension.toLowerCase(Locale.ROOT);
        try (Stream<Path> pathStream = java.nio.file.Files.walk(directory)) {
            return pathStream
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(normalizedExtension))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan directory: " + directory, e);
        }
    }

    default void defaultValidate(Path directory, TypeFromDocs sourceType, TypeToDocs targetType) {
        if (directory == null || sourceType == null || targetType == null) {
            throw new IllegalArgumentException("Directory and types are required");
        }
        if (!java.nio.file.Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory: " + directory);
        }

    }
}
