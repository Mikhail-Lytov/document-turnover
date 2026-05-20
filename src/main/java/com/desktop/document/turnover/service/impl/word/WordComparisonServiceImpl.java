package com.desktop.document.turnover.service.impl.word;

import com.desktop.document.turnover.service.api.word.WordComparisonService;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class WordComparisonServiceImpl implements WordComparisonService {

    private static final int WD_COMPARE_DESTINATION_NEW = 2;
    private static final int WD_GRANULARITY_WORD_LEVEL = 1;
    private static final String REVISED_AUTHOR = "DocumentTurnover";

    @Override
    public List<ComparisonTarget> findComparisonTargets(Path sourcePath, Path backupPath) {
        if (sourcePath == null || backupPath == null) {
            return List.of();
        }

        Path normalizedSource = sourcePath.toAbsolutePath().normalize();
        Path normalizedBackup = backupPath.toAbsolutePath().normalize();

        if (Files.isRegularFile(normalizedSource) && Files.isRegularFile(normalizedBackup)) {
            if (!isWordDocument(normalizedSource) || !isWordDocument(normalizedBackup)) {
                return List.of();
            }
            return List.of(new ComparisonTarget(normalizedSource.getFileName().toString(), normalizedBackup, normalizedSource));
        }

        if (!Files.isDirectory(normalizedSource) || !Files.isDirectory(normalizedBackup)) {
            return List.of();
        }

        try (Stream<Path> backupFiles = Files.walk(normalizedBackup)) {
            return backupFiles
                    .filter(Files::isRegularFile)
                    .filter(this::isWordDocument)
                    .map(backupFile -> toComparisonTarget(normalizedSource, normalizedBackup, backupFile))
                    .flatMap(Stream::ofNullable)
                    .sorted(Comparator.comparing(ComparisonTarget::fileName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Не удалось прочитать файлы бэкапа: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void openComparison(Path originalFile, Path revisedFile) {
        validateWordFile(originalFile, "Файл из бэкапа");
        validateWordFile(revisedFile, "Новый файл");

        ComThread.InitSTA();
        ActiveXComponent word = null;
        Dispatch originalDocument = null;
        Dispatch revisedDocument = null;
        boolean comparisonOpened = false;

        try {
            word = new ActiveXComponent("Word.Application");
            word.setProperty("Visible", new Variant(true));
            word.setProperty("DisplayAlerts", new Variant(0));

            Dispatch documents = word.getProperty("Documents").toDispatch();
            originalDocument = openReadOnly(documents, originalFile);
            revisedDocument = openReadOnly(documents, revisedFile);

            Dispatch comparisonDocument = Dispatch.call(
                    word,
                    "CompareDocuments",
                    originalDocument,
                    revisedDocument,
                    WD_COMPARE_DESTINATION_NEW,
                    WD_GRANULARITY_WORD_LEVEL,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    REVISED_AUTHOR,
                    true
            ).toDispatch();

            Dispatch.call(comparisonDocument, "Activate");
            Dispatch.call(word, "Activate");
            comparisonOpened = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось открыть сравнение в Microsoft Word: " + safeMessage(exception), exception);
        } finally {
            closeDocument(originalDocument);
            closeDocument(revisedDocument);
            if (!comparisonOpened && word != null) {
                word.invoke("Quit", 0);
            }
            ComThread.Release();
        }
    }

    private ComparisonTarget toComparisonTarget(Path sourcePath, Path backupPath, Path backupFile) {
        Path relativePath = backupPath.relativize(backupFile);
        Path revisedFile = sourcePath.resolve(relativePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(revisedFile) || !isWordDocument(revisedFile)) {
            return null;
        }
        return new ComparisonTarget(relativePath.toString(), backupFile.toAbsolutePath().normalize(), revisedFile);
    }

    private Dispatch openReadOnly(Dispatch documents, Path file) {
        return Dispatch.call(documents, "Open", file.toAbsolutePath().toString(), false, true).toDispatch();
    }

    private void validateWordFile(Path file, String fieldName) {
        if (file == null) {
            throw new IllegalArgumentException(fieldName + " не указан.");
        }

        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedFile)) {
            throw new IllegalArgumentException(fieldName + " не найден: " + normalizedFile);
        }
        if (!isWordDocument(normalizedFile)) {
            throw new IllegalArgumentException(fieldName + " должен быть DOC или DOCX: " + normalizedFile);
        }
    }

    private boolean isWordDocument(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".doc") || fileName.endsWith(".docx");
    }

    private void closeDocument(Dispatch document) {
        if (document == null) {
            return;
        }

        try {
            Dispatch.call(document, "Close", false);
        } catch (Exception ignored) {
            // Word may already close helper documents after comparison.
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
