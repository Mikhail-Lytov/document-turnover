package com.desktop.document.turnover.service.impl.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.domain.enums.TypeToDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

@Service
@RequiredArgsConstructor
public class ConverterWordServiceImpl implements ConverterService {


    @Override
    public int getFilesCountByType(Path directory, TypeFromDocs sourceType) {
        if (directory == null || sourceType == null || !java.nio.file.Files.isDirectory(directory)) {
            return 0;
        }
        return findFilesByExtension(directory, sourceType.getName()).size();
    }

    @Override
    public int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType) {
        return convertDirectory(directory, sourceType, targetType, null);
    }

    @Override
    public int convertDirectory(Path directory, TypeFromDocs sourceType, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
        defaultValidate(directory, sourceType, targetType);

        List<Path> filesToConvert = findFilesByExtension(directory, sourceType.getName());
        if (filesToConvert.isEmpty()) {
            return 0;
        }

        if (Objects.equals(sourceType.getName(), targetType.getName())) {
            notifyProcessedFiles(processedFilesConsumer, filesToConvert.size());
            return 0;
        }

        return convertWithWord(filesToConvert, targetType, processedFilesConsumer);
    }

    @Override
    public int convertFile(Path file, TypeFromDocs sourceType, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
        defaultValidateFile(file, sourceType, targetType);

        if (getFileCountByType(file, sourceType) == 0) {
            return 0;
        }

        if (Objects.equals(sourceType.getName(), targetType.getName())) {
            notifyProcessedFiles(processedFilesConsumer, 1);
            return 0;
        }

        return convertWithWord(List.of(file), targetType, processedFilesConsumer);
    }

    @Override
    public List<TypeFromDocs> getTypes() {
        return List.of(
                TypeFromDocs.DOS,
                TypeFromDocs.DOCX,
                TypeFromDocs.DOC,
                TypeFromDocs.RTF
        );
    }

    private int convertWithWord(List<Path> sourceFiles, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
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
                    notifyProcessedFiles(processedFilesConsumer, 1);
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

    private Path buildTargetPath(Path sourcePath, TypeToDocs targetType) {
        String fileName = sourcePath.getFileName().toString();
        int extensionPosition = fileName.lastIndexOf('.');
        String baseName = extensionPosition > 0 ? fileName.substring(0, extensionPosition) : fileName;
        return sourcePath.getParent().resolve(baseName + "." + targetType.getName());
    }

    private void notifyProcessedFiles(IntConsumer processedFilesConsumer, int processedFiles) {
        if (processedFilesConsumer != null && processedFiles > 0) {
            processedFilesConsumer.accept(processedFiles);
        }
    }

}
