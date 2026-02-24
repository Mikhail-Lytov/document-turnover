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
        defaultValidate(directory, sourceType, targetType);

        if (Objects.equals(sourceType.getName(), targetType.getName())) {
            return 0;
        }

        List<Path> filesToConvert = findFilesByExtension(directory, sourceType.getName());
        if (filesToConvert.isEmpty()) {
            return 0;
        }

        return convertWithExcel(filesToConvert, targetType);
    }


    @Override
    public List<TypeFromDocs> getTypes() {
        return List.of(
                TypeFromDocs.EXCEL
        );
    }

    private int convertWithExcel(List<Path> filesToConvert, TypeToDocs targetType) {
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
}
