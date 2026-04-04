package com.vrbs.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReceiptPdfUtil {

    private ReceiptPdfUtil() {
    }

    public static void writeSimpleReceipt(Path output, String title, List<String> lines) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            float y = 750;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 16);
                cs.newLineAtOffset(50, y);
                cs.showText(title);
                cs.endText();
                y -= 30;
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                cs.endText();
                y -= 24;
                cs.setFont(font, 11);
                for (String line : lines) {
                    if (line == null) {
                        continue;
                    }
                    String chunk = line.length() > 90 ? line.substring(0, 90) : line;
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(chunk);
                    cs.endText();
                    y -= 16;
                    if (y < 60) {
                        break;
                    }
                }
            }
            doc.save(output.toFile());
        }
    }
}
