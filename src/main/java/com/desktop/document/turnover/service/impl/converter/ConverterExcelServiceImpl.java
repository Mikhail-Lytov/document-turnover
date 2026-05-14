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
public class ConverterExcelServiceImpl implements ConverterService {
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

        return convertWithExcel(filesToConvert, targetType, processedFilesConsumer);
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

        return convertWithExcel(List.of(file), targetType, processedFilesConsumer);
    }


    @Override
    public List<TypeFromDocs> getTypes() {
        return List.of(
                TypeFromDocs.EXCEL
        );
    }

    private int convertWithExcel(List<Path> filesToConvert, TypeToDocs targetType, IntConsumer processedFilesConsumer) {
        ComThread.InitSTA();
        ActiveXComponent excel = null;
        Dispatch workbooks = null;
        int successCount = 0;

        try {
            excel = new ActiveXComponent("Excel.Application");
            excel.setProperty("Visible", new Variant(false));
            excel.setProperty("DisplayAlerts", new Variant(false));
            workbooks = excel.getProperty("Workbooks").toDispatch();

            for (Path sourcePath : filesToConvert) {
                Dispatch workbook = null;
                try {
                    workbook = Dispatch.call(
                            workbooks,
                            "Open",
                            sourcePath.toAbsolutePath().toString(),
                            false,
                            true
                    ).toDispatch();

                    Path targetPath = buildTargetPath(sourcePath, targetType);
                    if (Objects.equals(targetType.getName(), "pdf")) {
                        Dispatch.call(
                                workbook,
                                "ExportAsFixedFormat",
                                0,
                                targetPath.toAbsolutePath().toString()
                        );
                    } else {
                        Dispatch.call(
                                workbook,
                                "SaveAs",
                                targetPath.toAbsolutePath().toString()
                        );
                    }
                    successCount++;
                } catch (Exception ignored) {
                    // Continue converting other files even if one file fails.
                } finally {
                    notifyProcessedFiles(processedFilesConsumer, 1);
                    if (workbook != null) {
                        Dispatch.call(workbook, "Close", false);
                    }
                }
            }
        } finally {
            if (excel != null) {
                excel.invoke("Quit");
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
