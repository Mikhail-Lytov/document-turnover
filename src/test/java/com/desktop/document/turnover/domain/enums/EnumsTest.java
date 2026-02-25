package com.desktop.document.turnover.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumsTest {

    @Test
    void typeFromDocsToStringReturnsName() {
        assertEquals("docx", TypeFromDocs.DOCX.toString());
        assertEquals("doc", TypeFromDocs.DOC.toString());
        assertEquals("HTML", TypeFromDocs.DOS.toString());
        assertEquals("rtf", TypeFromDocs.RTF.toString());
        assertEquals("XLSX", TypeFromDocs.EXCEL.toString());
    }

    @Test
    void typeToDocsToStringReturnsName() {
        assertEquals("pdf", TypeToDocs.PDF.toString());
    }

    @Test
    void resourcesSystemTypeToStringReturnsLocalizedName() {
        assertEquals("Файл", ResourcesSystemType.FILE.toString());
        assertEquals("Директория", ResourcesSystemType.DIRECTORY.toString());
    }
}
