package com.desktop.document.turnover.service.api.alphabet;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public interface AlphabetReplaceService {

    SearchOperationResult searchInDocuments(Path directory, String searchText);

    default ReplaceOperationResult replaceInDocuments(Path directory, Path alphabetFile) {
        return replaceInDocuments(directory, alphabetFile, null);
    }

    ReplaceOperationResult replaceInDocuments(Path directory, Path alphabetFile, BiConsumer<Integer, Integer> progressCallback);

    default ReplaceOperationResult replaceInDocuments(Path directory, String alphabetContent) {
        return replaceInDocuments(directory, alphabetContent, null);
    }

    ReplaceOperationResult replaceInDocuments(Path directory, String alphabetContent, BiConsumer<Integer, Integer> progressCallback);

    record ReplacementRule(String find, String replace) {
    }

    record SearchFileResult(String fileName, int matches, List<Integer> pages) {
    }

    record SearchOperationResult(
            int filesScanned,
            int totalMatches,
            List<SearchFileResult> matches,
            List<String> errors,
            String report
    ) {
    }

    record ReplaceOperationResult(
            int filesScanned,
            int totalReplacements,
            int rulesLoaded,
            Path sourcePath,
            Path backupPath,
            List<Path> changedFiles,
            Path logFile,
            Path contextLogFile,
            List<String> errors,
            String report
    ) {
    }
}
