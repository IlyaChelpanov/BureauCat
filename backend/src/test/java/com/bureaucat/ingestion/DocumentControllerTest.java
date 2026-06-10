package com.bureaucat.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({IngestionService.class, SourceTypeDetector.class})
class DocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void uploadTextPdfReturnsCreatedWithSourceType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "brief.pdf", "application/pdf",
                PdfFixtures.pdfWithText(
                        "Sehr geehrte Damen und Herren, hiermit erhalten Sie Ihre Rechnung über 129,90 EUR, zahlbar bis 15.06.2026."));

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("brief.pdf"))
                .andExpect(jsonPath("$.sourceType").value("PDF_TEXT"));
    }

    @Test
    void uploadPngReturnsImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("IMAGE"));
    }

    @Test
    void uploadUnsupportedTypeReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "letter.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadEmptyFileReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest());
    }
}
