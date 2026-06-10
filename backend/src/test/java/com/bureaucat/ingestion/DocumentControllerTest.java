package com.bureaucat.ingestion;

import com.bureaucat.cards.CardStatus;
import com.bureaucat.cards.DocType;
import com.bureaucat.cards.DocumentCard;
import com.bureaucat.cards.SourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    IngestionService ingestionService;

    @Test
    void uploadSupportedFileReturns201WithCard() throws Exception {
        DocumentCard card = new DocumentCard();
        card.setId(UUID.randomUUID());
        card.setOriginalFilename("brief.pdf");
        card.setSourceType(SourceType.PDF_TEXT);
        card.setDocType(DocType.INVOICE);
        card.setStatus(CardStatus.NEW);

        when(ingestionService.isSupported("application/pdf")).thenReturn(true);
        when(ingestionService.ingest(eq("brief.pdf"), eq("application/pdf"), any())).thenReturn(card);

        MockMultipartFile file = new MockMultipartFile("file", "brief.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(card.getId().toString()))
                .andExpect(jsonPath("$.originalFilename").value("brief.pdf"))
                .andExpect(jsonPath("$.docType").value("INVOICE"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void uploadUnsupportedTypeReturns400() throws Exception {
        when(ingestionService.isSupported(anyString())).thenReturn(false);
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
