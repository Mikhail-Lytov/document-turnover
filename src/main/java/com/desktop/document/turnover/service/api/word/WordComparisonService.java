package com.desktop.document.turnover.service.api.word;

import java.nio.file.Path;
import java.util.List;

public interface WordComparisonService {

    List<ComparisonTarget> findComparisonTargets(Path sourcePath, Path backupPath);

    void openComparison(Path originalFile, Path revisedFile);

    record ComparisonTarget(String fileName, Path originalFile, Path revisedFile) {
    }
}
