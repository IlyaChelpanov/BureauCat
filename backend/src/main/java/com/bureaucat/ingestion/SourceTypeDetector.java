package com.bureaucat.ingestion;

import com.bureaucat.cards.SourceType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

@Component
public class SourceTypeDetector {

    static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    /** Average extracted chars per page below this means the PDF has no usable text layer. */
    private static final int MIN_CHARS_PER_PAGE = 50;

    public boolean isSupported(String contentType) {
        return contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }

    public SourceType detect(String contentType, byte[] content) {
        if (!isSupported(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        if (!"application/pdf".equals(contentType)) {
            return SourceType.IMAGE;
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            int pages = Math.max(document.getNumberOfPages(), 1);
            return text.strip().length() / pages >= MIN_CHARS_PER_PAGE
                    ? SourceType.PDF_TEXT
                    : SourceType.PDF_SCAN;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF", e);
        }
    }
}
