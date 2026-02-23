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
        defaultValidate(directory, sourceType, targetType);

        if (Objects.equals(sourceType.getName(), targetType.getName())) {
            return 0;
        }

        List<Path> filesToConvert = findFilesByExtension(directory, sourceType.getName());
        if (filesToConvert.isEmpty()) {
            return 0;
        }

        return convertWithWord(filesToConvert, targetType);
    }

    @Override
    public List<TypeFromDocs> getTypes() {
        return List.of(
                TypeFromDocs.DOS,
                TypeFromDocs.DOCX,
                TypeFromDocs.DOC
        );
    }

    private int convertWithWord(List<Path> sourceFiles, TypeToDocs targetType) {
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

    private Path buildTargetPath(Path sourcePath, TypeToDocs targetType) {
        String fileName = sourcePath.getFileName().toString();
        int extensionPosition = fileName.lastIndexOf('.');
        String baseName = extensionPosition > 0 ? fileName.substring(0, extensionPosition) : fileName;
        return sourcePath.getParent().resolve(baseName + "." + targetType.getName());
    }

}
