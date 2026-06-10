package com.bureaucat.ingestion;

import com.bureaucat.analysis.AnalysisCost;
import com.bureaucat.analysis.AnalysisCostRepository;
import com.bureaucat.analysis.AnalysisOutcome;
import com.bureaucat.analysis.AnalysisResult;
import com.bureaucat.analysis.DocumentAnalyzer;
import com.bureaucat.cards.CardStatus;
import com.bureaucat.cards.Confidence;
import com.bureaucat.cards.DocType;
import com.bureaucat.cards.DocumentCard;
import com.bureaucat.cards.DocumentCardRepository;
import com.bureaucat.cards.RequiredAction;
import com.bureaucat.cards.SourceType;
import com.bureaucat.cards.Urgency;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentUploadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentCardRepository cardRepository;

    @Autowired
    AnalysisCostRepository costRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DocumentAnalyzer documentAnalyzer;

    @Test
    void uploadPersistsCardWithQuotesAndCostRow() throws Exception {
        AnalysisResult result = new AnalysisResult(
                "de", DocType.REMINDER_MAHNUNG, "Stadtwerke München", "Versorger",
                LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 15),
                new BigDecimal("129.90"), "EUR",
                Map.of("kundennummer", "KD-12345"),
                "Напоминание об оплате счёта за электричество.",
                RequiredAction.PAY,
                List.of("Оплатить 129.90 EUR до 15.06.2026"),
                Urgency.HIGH, Confidence.HIGH,
                Map.of("deadline", "Bitte zahlen Sie bis zum 15.06.2026",
                        "amount", "Offener Betrag: 129,90 EUR",
                        "required_action", "Wir bitten um umgehende Zahlung"),
                "Clear text layer.");
        AnalysisOutcome outcome = new AnalysisOutcome(
                result, Map.of("doc_type", "REMINDER_MAHNUNG"), "claude-opus-4-8", 2000, 500);
        when(documentAnalyzer.analyze(eq(SourceType.PDF_TEXT), eq("application/pdf"), any()))
                .thenReturn(outcome);

        MockMultipartFile file = new MockMultipartFile("file", "mahnung.pdf", "application/pdf",
                PdfFixtures.pdfWithText(
                        "Mahnung: Bitte zahlen Sie den offenen Betrag von 129,90 EUR bis zum 15.06.2026. Kundennummer KD-12345."));

        MvcResult mvcResult = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docType").value("REMINDER_MAHNUNG"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.evidenceQuotes.deadline").value("Bitte zahlen Sie bis zum 15.06.2026"))
                .andReturn();

        UUID cardId = UUID.fromString(
                objectMapper.readTree(mvcResult.getResponse().getContentAsByteArray()).get("id").asText());

        DocumentCard saved = cardRepository.findById(cardId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CardStatus.NEW);
        assertThat(saved.getDeadline()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(saved.getEvidenceQuotes()).containsKeys("deadline", "amount", "required_action");
        assertThat(saved.getRawAnalysis()).containsEntry("doc_type", "REMINDER_MAHNUNG");

        List<AnalysisCost> costs = costRepository.findByCardId(cardId);
        assertThat(costs).hasSize(1);
        AnalysisCost cost = costs.get(0);
        assertThat(cost.getModel()).isEqualTo("claude-opus-4-8");
        assertThat(cost.getInputTokens()).isEqualTo(2000);
        assertThat(cost.getOutputTokens()).isEqualTo(500);
        // 2000 * $5/M + 500 * $25/M = $0.01 + $0.0125 = $0.0225
        assertThat(cost.getCostUsd()).isEqualByComparingTo("0.022500");
    }
}
