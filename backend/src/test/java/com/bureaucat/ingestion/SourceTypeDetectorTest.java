package com.bureaucat.ingestion;

import com.bureaucat.cards.SourceType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceTypeDetectorTest {

    private final SourceTypeDetector detector = new SourceTypeDetector();

    @Test
    void pdfWithTextLayerIsPdfText() throws IOException {
        byte[] pdf = PdfFixtures.pdfWithText(
                "Sehr geehrte Damen und Herren, bitte zahlen Sie den offenen Betrag von 129,90 EUR bis zum 15.06.2026.");
        assertThat(detector.detect("application/pdf", pdf)).isEqualTo(SourceType.PDF_TEXT);
    }

    @Test
    void pdfWithoutTextLayerIsPdfScan() throws IOException {
        byte[] pdf = PdfFixtures.pdfWithoutText();
        assertThat(detector.detect("application/pdf", pdf)).isEqualTo(SourceType.PDF_SCAN);
    }

    @Test
    void pdfWithTooLittleTextIsPdfScan() throws IOException {
        byte[] pdf = PdfFixtures.pdfWithText("Rechnung");
        assertThat(detector.detect("application/pdf", pdf)).isEqualTo(SourceType.PDF_SCAN);
    }

    @Test
    void jpegAndPngAreImage() {
        byte[] anyBytes = {1, 2, 3};
        assertThat(detector.detect("image/jpeg", anyBytes)).isEqualTo(SourceType.IMAGE);
        assertThat(detector.detect("image/png", anyBytes)).isEqualTo(SourceType.IMAGE);
    }

    @Test
    void unsupportedContentTypeRejected() {
        assertThatThrownBy(() -> detector.detect("text/plain", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(detector.isSupported("application/zip")).isFalse();
        assertThat(detector.isSupported(null)).isFalse();
    }
}
