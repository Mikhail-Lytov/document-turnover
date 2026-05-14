package com.desktop.document.turnover.service.api.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ConverterService {

    int getFilesCountByType(Path directory, TypeFromDocs sourceType);

    default int getFileCountByType(Path file, TypeFromDocs sourceType) {
        if (file == null || sourceType == null || !java.nio.file.Files.isRegularFile(file)) {
            return 0;
        }

        String normalizedExtension = "." + sourceType.getName().toLowerCase(Locale.ROOT);
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(normalizedExtension) ? 1 : 0;
    }

    int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType);

    default int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
        return convertDirectory(directory, sourceType, targetType);
    }

    default int convertFile(Path file, TypeFromDocs sourceType, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
        throw new UnsupportedOperationException("Single file conversion is not supported");
    }

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

    default void defaultValidateFile(Path file, TypeFromDocs sourceType, TypeToDocs targetType) {
        if (file == null || sourceType == null || targetType == null) {
            throw new IllegalArgumentException("File and types are required");
        }
        if (!java.nio.file.Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path is not a file: " + file);
        }
    }
}
