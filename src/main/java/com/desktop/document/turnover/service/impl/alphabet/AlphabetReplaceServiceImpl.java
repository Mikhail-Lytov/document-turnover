package com.desktop.document.turnover.service.impl.alphabet;

import com.desktop.document.turnover.service.api.alphabet.AlphabetReplaceService;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class AlphabetReplaceServiceImpl implements AlphabetReplaceService {

    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("(.+?)\\s*->\\s*(.+)");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final int WD_FIND_STOP = 0;
    private static final int WD_COLLAPSE_END = 0;
    private static final int WD_ACTIVE_END_PAGE_NUMBER = 3;

    @Override
    public SearchOperationResult searchInDocuments(Path directory, String searchText) {
        validateDirectory(directory);
        validateSearchText(searchText);

        List<Path> files = listWordFiles(directory);
        if (files.isEmpty()) {
            String report = "=== РЕЗУЛЬТАТЫ ПОИСКА ===\nDOC/DOCX файлы не найдены в выбранной папке.";
            return new SearchOperationResult(0, 0, List.of(), List.of(), report);
        }

        List<SearchFileResult> searchResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalMatches = 0;

        ComThread.InitSTA();
        ActiveXComponent word = null;
        Dispatch documents = null;
        try {
            word = createWordApp();
            documents = word.getProperty("Documents").toDispatch();

            for (Path file : files) {
                Dispatch document = null;
                try {
                    document = openDocument(documents, file);
                    Dispatch.call(document, "Activate");

                    Dispatch selection = word.getProperty("Selection").toDispatch();
                    resetSelectionToStart(selection);

                    Dispatch find = Dispatch.get(selection, "Find").toDispatch();
                    configureFind(find, searchText, false);

                    int fileMatches = 0;
                    List<Integer> pages = new ArrayList<>();

                    while (executeFind(find)) {
                        fileMatches++;
                        Integer pageNumber = safePageNumber(selection);
                        if (pageNumber != null && pageNumber > 0) {
                            pages.add(pageNumber);
                        }
                    }

                    if (fileMatches > 0) {
                        totalMatches += fileMatches;
                        searchResults.add(new SearchFileResult(
                                file.getFileName().toString(),
                                fileMatches,
                                List.copyOf(pages)
                        ));
                    }
                } catch (Exception exception) {
                    errors.add(file.getFileName() + ": " + safeMessage(exception));
                } finally {
                    closeDocument(document);
                }
            }
        } finally {
            closeWord(word);
            ComThread.Release();
        }

        String report = buildSearchReport(searchText, files.size(), totalMatches, searchResults, errors);
        return new SearchOperationResult(files.size(), totalMatches, List.copyOf(searchResults), List.copyOf(errors), report);
    }

    @Override
    public ReplaceOperationResult replaceInDocuments(Path directory, Path alphabetFile) {
        validateDirectory(directory);
        validateAlphabetFile(alphabetFile);

        List<ReplacementRule> replacements = loadReplacements(alphabetFile);
        if (replacements.isEmpty()) {
            throw new IllegalArgumentException("Файл алфавита не содержит ни одной корректной замены в формате 'что -> на что'.");
        }

        return replaceInDocuments(directory, replacements, alphabetFile.toAbsolutePath().toString());
    }

    @Override
    public ReplaceOperationResult replaceInDocuments(Path directory, String alphabetContent) {
        validateDirectory(directory);
        validateAlphabetContent(alphabetContent);

        List<ReplacementRule> replacements = loadReplacements(alphabetContent);
        if (replacements.isEmpty()) {
            throw new IllegalArgumentException("Алфавит не содержит ни одной корректной замены в формате 'что -> на что'.");
        }

        return replaceInDocuments(directory, replacements, "Встроенный редактор");
    }

    private ReplaceOperationResult replaceInDocuments(Path directory, List<ReplacementRule> replacements, String alphabetSource) {
        List<Path> files = listWordFiles(directory);
        if (files.isEmpty()) {
            String report = "=== РЕЗУЛЬТАТ ===\nDOC/DOCX файлы не найдены в выбранной папке.";
            return new ReplaceOperationResult(0, 0, replacements.size(), directory, directory, null, null, List.of(), report);
        }

        List<String> errors = new ArrayList<>();
        Path backupRootPath = createUniqueBackupPath(directory);
        Path backupPath = backupRootPath.resolve("old_file");
        Path logsPath = backupRootPath.resolve("logs");
        try {
            copyDirectory(directory, backupPath);
            Files.createDirectories(logsPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось создать бэкап папки: " + safeMessage(exception), exception);
        }

        String timestamp = LocalDateTime.now().format(LOG_TS_FORMAT);
        Path logFile = logsPath.resolve("replacements_log_" + timestamp + ".txt");
        Path contextLogFile = logsPath.resolve("context_log_" + timestamp + ".txt");

        List<String> mainLogLines = new ArrayList<>();
        List<String> contextLogLines = new ArrayList<>();

        mainLogLines.add("Лог замен в документах Word");
        mainLogLines.add("Дата выполнения: " + LocalDateTime.now());
        mainLogLines.add("Путь к документам: " + directory.toAbsolutePath());
        mainLogLines.add("Бэкап создан: " + backupPath.toAbsolutePath());
        mainLogLines.add("Источник алфавита: " + alphabetSource);
        mainLogLines.add("Загружено замен: " + replacements.size());
        mainLogLines.add("Найдено файлов: " + files.size());
        mainLogLines.add("");
        mainLogLines.add("АЛФАВИТ ЗАМЕН:");
        for (ReplacementRule replacement : replacements) {
            mainLogLines.add("  - '" + replacement.find() + "' -> '" + replacement.replace() + "'");
        }
        mainLogLines.add("");

        contextLogLines.add("Лог контекстов замен в документах Word");
        contextLogLines.add("Дата выполнения: " + LocalDateTime.now());
        contextLogLines.add("Путь к документам: " + directory.toAbsolutePath());
        contextLogLines.add("Бэкап создан: " + backupPath.toAbsolutePath());
        contextLogLines.add("Источник алфавита: " + alphabetSource);
        contextLogLines.add("Загружено замен: " + replacements.size());
        contextLogLines.add("Найдено файлов: " + files.size());
        contextLogLines.add("");
        contextLogLines.add("ПРИМЕЧАНИЕ: ||текст|| - выделение замененного/нового текста");
        contextLogLines.add("");

        Map<String, ReplacementAggregate> globalStats = new LinkedHashMap<>();
        int totalReplacements = 0;

        ComThread.InitSTA();
        ActiveXComponent word = null;
        Dispatch documents = null;

        try {
            word = createWordApp();
            documents = word.getProperty("Documents").toDispatch();

            for (Path file : files) {
                Dispatch document = null;
                int fileReplacements = 0;
                List<String> perFileMain = new ArrayList<>();
                List<String> perFileContext = new ArrayList<>();

                mainLogLines.add("Файл: " + file.getFileName());
                contextLogLines.add("Файл: " + file.getFileName());

                try {
                    document = openDocument(documents, file);
                    Dispatch.call(document, "Activate");

                    Dispatch selection = word.getProperty("Selection").toDispatch();

                    for (ReplacementRule replacement : replacements) {
                        resetSelectionToStart(selection);
                        Dispatch find = Dispatch.get(selection, "Find").toDispatch();
                        configureFind(find, replacement.find(), true);

                        int replacementCount = 0;
                        List<Integer> pages = new ArrayList<>();
                        List<ReplacementContext> contexts = new ArrayList<>();

                        while (executeFind(find)) {
                            replacementCount++;

                            Integer pageNumber = safePageNumber(selection);
                            if (pageNumber != null && pageNumber > 0) {
                                pages.add(pageNumber);
                            }

                            ReplacementContext context = captureReplacementContext(selection, replacement.find(), replacement.replace());
                            if (context != null) {
                                contexts.add(context);
                            }

                            replaceCurrentSelection(selection, replacement.replace());
                        }

                        if (replacementCount > 0) {
                            fileReplacements += replacementCount;

                            String statKey = replacement.find() + " -> " + replacement.replace();
                            ReplacementAggregate aggregate = globalStats.computeIfAbsent(statKey, key -> new ReplacementAggregate());
                            aggregate.count += replacementCount;
                            aggregate.pages.addAll(pages);
                            aggregate.contexts.addAll(contexts);

                            Set<Integer> uniquePages = new TreeSet<>(pages);
                            String pageInfo = uniquePages.isEmpty()
                                    ? ""
                                    : " (страницы: " + joinPages(uniquePages) + ")";

                            String message = "  - Заменено: '" + replacement.find() + "' -> '" + replacement.replace() + "' (" + replacementCount + " раз" + pageInfo + ")";
                            perFileMain.add(message);

                            if (!contexts.isEmpty()) {
                                perFileContext.add("  Замена: '" + replacement.find() + "' -> '" + replacement.replace() + "' (" + replacementCount + " раз" + pageInfo + ")");
                                for (ReplacementContext replacementContext : contexts) {
                                    perFileContext.add("    Было: " + replacementContext.original());
                                    perFileContext.add("    Стало: " + replacementContext.replaced());
                                    perFileContext.add("");
                                }
                            }
                        }
                    }

                    Dispatch.call(document, "Save");
                    totalReplacements += fileReplacements;

                    if (perFileMain.isEmpty()) {
                        mainLogLines.add("  - Замены не найдены");
                    } else {
                        mainLogLines.addAll(perFileMain);
                    }
                    mainLogLines.add("  Всего замен в файле: " + fileReplacements);
                    mainLogLines.add("");

                    if (!perFileContext.isEmpty()) {
                        contextLogLines.addAll(perFileContext);
                    }
                    contextLogLines.add("  Всего замен в файле: " + fileReplacements);
                    contextLogLines.add("");
                } catch (Exception exception) {
                    String errorMessage = "Ошибка в файле " + file.getFileName() + ": " + safeMessage(exception);
                    errors.add(errorMessage);
                    mainLogLines.add("  " + errorMessage);
                    mainLogLines.add("");
                    contextLogLines.add("  " + errorMessage);
                    contextLogLines.add("");
                } finally {
                    closeDocument(document);
                }
            }
        } finally {
            closeWord(word);
            ComThread.Release();
        }

        mainLogLines.add("=".repeat(50));
        mainLogLines.add("ОБЩАЯ СТАТИСТИКА");
        mainLogLines.add("Обработано файлов: " + files.size());
        mainLogLines.add("Всего выполнено замен: " + totalReplacements);
        mainLogLines.add("Бэкап создан: " + backupPath.toAbsolutePath());
        mainLogLines.add("");

        contextLogLines.add("=".repeat(50));
        contextLogLines.add("ОБЩАЯ СТАТИСТИКА");
        contextLogLines.add("Обработано файлов: " + files.size());
        contextLogLines.add("Всего выполнено замен: " + totalReplacements);
        contextLogLines.add("Бэкап создан: " + backupPath.toAbsolutePath());
        contextLogLines.add("");

        appendDetailedStats(mainLogLines, globalStats);

        try {
            Files.write(logFile, mainLogLines, StandardCharsets.UTF_8);
            Files.write(contextLogFile, contextLogLines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            errors.add("Не удалось сохранить лог-файлы: " + safeMessage(exception));
        }

        String report = buildReplaceReport(
                files.size(),
                replacements.size(),
                totalReplacements,
                backupPath,
                logFile,
                contextLogFile,
                globalStats,
                errors
        );

        return new ReplaceOperationResult(
                files.size(),
                totalReplacements,
                replacements.size(),
                directory,
                backupPath,
                logFile,
                contextLogFile,
                List.copyOf(errors),
                report
        );
    }

    private void appendDetailedStats(List<String> target, Map<String, ReplacementAggregate> globalStats) {
        if (globalStats.isEmpty()) {
            target.add("Замены не выполнены.");
            return;
        }

        target.add("ДЕТАЛЬНАЯ СТАТИСТИКА ПО ТИПАМ ЗАМЕН:");
        globalStats.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue().count, left.getValue().count))
                .forEach(entry -> {
                    String pageInfo = entry.getValue().pages.isEmpty()
                            ? ""
                            : " - страницы: " + joinPages(entry.getValue().pages);
                    target.add("  - " + entry.getKey() + ": " + entry.getValue().count + " раз" + pageInfo);
                });
    }

    private String buildReplaceReport(
            int filesScanned,
            int rulesLoaded,
            int totalReplacements,
            Path backupPath,
            Path logFile,
            Path contextLogFile,
            Map<String, ReplacementAggregate> globalStats,
            List<String> errors
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("=== РЕЗУЛЬТАТ ===");
        lines.add("Обработано файлов: " + filesScanned);
        lines.add("Загружено замен: " + rulesLoaded);
        lines.add("Всего замен: " + totalReplacements);
        lines.add("Бэкап создан: " + backupPath.toAbsolutePath());
        lines.add("Основной лог-файл: " + logFile.toAbsolutePath());
        lines.add("Лог контекстов: " + contextLogFile.toAbsolutePath());

        if (!globalStats.isEmpty()) {
            lines.add("");
            lines.add("ДЕТАЛЬНАЯ СТАТИСТИКА ПО ТИПАМ ЗАМЕН:");
            globalStats.entrySet().stream()
                    .sorted((left, right) -> Integer.compare(right.getValue().count, left.getValue().count))
                    .forEach(entry -> {
                        String pageInfo = entry.getValue().pages.isEmpty()
                                ? ""
                                : " (страницы: " + joinPages(entry.getValue().pages) + ")";
                        lines.add("  " + entry.getKey() + ": " + entry.getValue().count + " раз" + pageInfo);
                    });
        }

        if (!errors.isEmpty()) {
            lines.add("");
            lines.add("ОШИБКИ:");
            errors.forEach(error -> lines.add("  - " + error));
        }

        return String.join("\n", lines);
    }

    private String buildSearchReport(
            String searchText,
            int filesScanned,
            int totalMatches,
            List<SearchFileResult> matches,
            List<String> errors
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("=== РЕЗУЛЬТАТЫ ПОИСКА ===");
        lines.add("Искомый текст: '" + searchText + "'");
        lines.add("Всего файлов проверено: " + filesScanned);
        lines.add("Всего совпадений: " + totalMatches);

        if (!matches.isEmpty()) {
            lines.add("");
            lines.add("Найдено в файлах:");
            for (SearchFileResult result : matches) {
                String pageInfo = result.pages().isEmpty()
                        ? ""
                        : " (страницы: " + joinPages(result.pages()) + ")";
                lines.add("  - " + result.fileName() + ": " + result.matches() + " совпадений" + pageInfo);
            }
        } else {
            lines.add("");
            lines.add("Совпадений не найдено.");
        }

        if (!errors.isEmpty()) {
            lines.add("");
            lines.add("ОШИБКИ:");
            errors.forEach(error -> lines.add("  - " + error));
        }

        return String.join("\n", lines);
    }

    private List<ReplacementRule> loadReplacements(Path alphabetFile) {
        try {
            return parseReplacements(Files.readAllLines(alphabetFile, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать файл алфавита: " + safeMessage(exception), exception);
        }
    }

    private List<ReplacementRule> loadReplacements(String alphabetContent) {
        return parseReplacements(alphabetContent.lines().toList());
    }

    private List<ReplacementRule> parseReplacements(List<String> lines) {
        List<ReplacementRule> replacements = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher matcher = REPLACEMENT_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                continue;
            }

            String find = matcher.group(1).trim();
            String replace = matcher.group(2).trim();
            if (!find.isEmpty()) {
                replacements.add(new ReplacementRule(find, replace));
            }
        }
        return replacements;
    }

    private List<Path> listWordFiles(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isWordDocument)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать содержимое папки: " + safeMessage(exception), exception);
        }
    }

    private boolean isWordDocument(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".docx") || name.endsWith(".doc");
    }

    private Path createUniqueBackupPath(Path directory) {
        Path parent = directory.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Не удалось определить родительскую папку для: " + directory);
        }

        String baseBackupName = "backup";
        Path backupPath = parent.resolve(baseBackupName);

        if (!Files.exists(backupPath)) {
            return backupPath;
        }

        int counter = 1;
        while (Files.exists(backupPath)) {
            backupPath = parent.resolve(baseBackupName + " " + counter);
            counter++;
        }

        return backupPath;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Path destinationParent = destination.getParent();
                        if (destinationParent != null) {
                            Files.createDirectories(destinationParent);
                        }
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        } catch (IllegalStateException wrapped) {
            if (wrapped.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw wrapped;
        }
    }

    private ActiveXComponent createWordApp() {
        ActiveXComponent word = new ActiveXComponent("Word.Application");
        word.setProperty("Visible", new Variant(false));
        word.setProperty("DisplayAlerts", new Variant(0));
        return word;
    }

    private Dispatch openDocument(Dispatch documents, Path file) {
        return Dispatch.call(documents, "Open", file.toAbsolutePath().toString(), false, false).toDispatch();
    }

    private void closeDocument(Dispatch document) {
        if (document == null) {
            return;
        }

        try {
            Dispatch.call(document, "Close", false);
        } catch (Exception ignored) {
            // Ignore close errors for failed documents.
        }
    }

    private void closeWord(ActiveXComponent word) {
        if (word == null) {
            return;
        }

        try {
            word.invoke("Quit", 0);
        } catch (Exception ignored) {
            // Ignore close errors.
        }
    }

    private void resetSelectionToStart(Dispatch selection) {
        Dispatch.call(selection, "SetRange", 0, 0);
    }

    private void configureFind(Dispatch find, String text, boolean matchCase) {
        Dispatch.call(find, "ClearFormatting");

        Dispatch replacement = Dispatch.get(find, "Replacement").toDispatch();
        Dispatch.call(replacement, "ClearFormatting");

        Dispatch.put(find, "Text", text);
        Dispatch.put(find, "Forward", true);
        Dispatch.put(find, "Wrap", WD_FIND_STOP);
        Dispatch.put(find, "Format", false);
        Dispatch.put(find, "MatchCase", matchCase);
        Dispatch.put(find, "MatchWholeWord", false);
        Dispatch.put(find, "MatchWildcards", false);
        Dispatch.put(find, "MatchSoundsLike", false);
        Dispatch.put(find, "MatchAllWordForms", false);
    }

    private boolean executeFind(Dispatch find) {
        Variant result = Dispatch.call(find, "Execute");
        try {
            return result.getBoolean();
        } catch (Exception ignored) {
            return result.getInt() != 0;
        }
    }

    private Integer safePageNumber(Dispatch selection) {
        try {
            return Dispatch.call(selection, "Information", WD_ACTIVE_END_PAGE_NUMBER).getInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void replaceCurrentSelection(Dispatch selection, String replacement) {
        Dispatch selectedRange = Dispatch.get(selection, "Range").toDispatch();
        Dispatch.put(selectedRange, "Text", replacement);
        Dispatch.call(selection, "Collapse", WD_COLLAPSE_END);
    }

    private ReplacementContext captureReplacementContext(Dispatch selection, String findText, String replaceText) {
        try {
            Dispatch selectionRange = Dispatch.get(selection, "Range").toDispatch();

            int start = Dispatch.get(selectionRange, "Start").getInt();
            int end = Dispatch.get(selectionRange, "End").getInt();

            Dispatch contextRange = Dispatch.get(selectionRange, "Duplicate").toDispatch();
            Dispatch.put(contextRange, "Start", Math.max(0, start - 30));
            Dispatch.put(contextRange, "End", end + 40);

            String contextText = String.valueOf(Dispatch.get(contextRange, "Text"));
            if (contextText.isBlank()) {
                return null;
            }

            int position = contextText.indexOf(findText);
            if (position < 0) {
                return null;
            }

            int tailStart = Math.min(position + findText.length(), contextText.length());
            String original = "..."
                    + contextText.substring(0, position)
                    + "||"
                    + findText
                    + "||"
                    + contextText.substring(tailStart)
                    + "...";
            String replaced = "..."
                    + contextText.substring(0, position)
                    + "||"
                    + replaceText
                    + "||"
                    + contextText.substring(tailStart)
                    + "...";

            return new ReplacementContext(original, replaced);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String joinPages(Iterable<Integer> pages) {
        StringBuilder builder = new StringBuilder();
        for (Integer page : pages) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(page);
        }
        return builder.toString();
    }

    private void validateDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Не указана папка с документами.");
        }
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException("Путь не существует: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Указанный путь должен быть папкой: " + directory);
        }
    }

    private void validateSearchText(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            throw new IllegalArgumentException("Текст для поиска не может быть пустым.");
        }
    }

    private void validateAlphabetFile(Path alphabetFile) {
        if (alphabetFile == null) {
            throw new IllegalArgumentException("Не указан файл алфавита замен.");
        }
        if (!Files.exists(alphabetFile)) {
            throw new IllegalArgumentException("Файл алфавита не найден: " + alphabetFile);
        }
        if (!Files.isRegularFile(alphabetFile)) {
            throw new IllegalArgumentException("Путь к алфавиту должен указывать на файл: " + alphabetFile);
        }
    }

    private void validateAlphabetContent(String alphabetContent) {
        if (alphabetContent == null || alphabetContent.isBlank()) {
            throw new IllegalArgumentException("Алфавит замен не может быть пустым.");
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private static final class ReplacementAggregate {
        private int count;
        private final Set<Integer> pages = new TreeSet<>();
        private final List<ReplacementContext> contexts = new ArrayList<>();
    }

    private record ReplacementContext(String original, String replaced) {
    }
}
