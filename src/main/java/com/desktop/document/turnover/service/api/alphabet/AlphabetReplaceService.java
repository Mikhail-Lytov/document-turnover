package com.desktop.document.turnover.service.api.alphabet;

import java.nio.file.Path;
import java.util.List;

public interface AlphabetReplaceService {

    SearchOperationResult searchInDocuments(Path directory, String searchText);

    ReplaceOperationResult replaceInDocuments(Path directory, Path alphabetFile);

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
            Path logFile,
            Path contextLogFile,
            List<String> errors,
            String report
    ) {
    }
}
