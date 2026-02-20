package com.desktop.document.turnover.service.impl.converter;

import com.desktop.document.turnover.domain.enums.TypeDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConverterServiceImpl implements ConverterService {


    @Override
    public int getFilesCountByType(Path directory, TypeDocs sourceType) {
        if (directory == null || sourceType == null || !java.nio.file.Files.isDirectory(directory)) {
            return 0;
        }
        return findFilesByExtension(directory, sourceType.getName()).size();
    }

    @Override
    public int convertDirectory(Path directory, TypeDocs sourceType, TypeDocs targetType) {
        if (directory == null || sourceType == null || targetType == null) {
            throw new IllegalArgumentException("Directory and types are required");
        }
        if (!java.nio.file.Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory: " + directory);
        }
        if (sourceType == targetType) {
            return 0;
        }

        List<Path> filesToConvert = findFilesByExtension(directory, sourceType.getName());
        if (filesToConvert.isEmpty()) {
            return 0;
        }

        return convertWithWord(filesToConvert, targetType);
    }

    private List<Path> findFilesByExtension(Path directory, String extension) {
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

    private int convertWithWord(List<Path> sourceFiles, TypeDocs targetType) {
        ComThread.InitSTA();
        ActiveXComponent word = null;
        Dispatch documents = null;
        int successCount = 0;

        try {
            word = new ActiveXComponent("Word.Application");
            word.setProperty("Visible", new Variant(false));
            word.setProperty("DisplayAlerts", new Variant(0));
            documents = word.getProperty("Documents").toDispatch();

            for (Path sourcePath : sourceFiles) {
                Dispatch openedDocument = null;
                try {
                    openedDocument = Dispatch.call(
                            documents,
                            "Open",
                            sourcePath.toAbsolutePath().toString(),
                            false,
                            true
                    ).toDispatch();

                    Dispatch.call(
                            openedDocument,
                            "SaveAs2",
                            buildTargetPath(sourcePath, targetType).toAbsolutePath().toString(),
                            targetType.getWordFormat()
                    );
                    successCount++;
                } catch (Exception ignored) {
                    // Continue converting other files even if one file fails.
                } finally {
                    if (openedDocument != null) {
                        Dispatch.call(openedDocument, "Close", false);
                    }
                }
            }
        } finally {
            if (word != null) {
                word.invoke("Quit", 0);
            }
            ComThread.Release();
        }

        return successCount;
    }

    private Path buildTargetPath(Path sourcePath, TypeDocs targetType) {
        String fileName = sourcePath.getFileName().toString();
        int extensionPosition = fileName.lastIndexOf('.');
        String baseName = extensionPosition > 0 ? fileName.substring(0, extensionPosition) : fileName;
        return sourcePath.getParent().resolve(baseName + "." + targetType.getName());
    }

}
