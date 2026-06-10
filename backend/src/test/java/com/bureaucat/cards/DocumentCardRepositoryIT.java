package com.bureaucat.cards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DocumentCardRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    DocumentCardRepository repository;

    @Test
    void saveAndFindRoundtripIncludingJsonb() {
        DocumentCard card = new DocumentCard();
        card.setOriginalFilename("mahnung.pdf");
        card.setSourceType(SourceType.PDF_TEXT);
        card.setLanguage("de");
        card.setDocType(DocType.REMINDER_MAHNUNG);
        card.setSenderName("Stadtwerke München");
        card.setSenderCategory("Versorger");
        card.setDocumentDate(LocalDate.of(2026, 5, 20));
        card.setDeadline(LocalDate.of(2026, 6, 15));
        card.setAmountValue(new BigDecimal("129.90"));
        card.setAmountCurrency("EUR");
        card.setReferenceNumbers(Map.of("kundennummer", "KD-12345", "rechnungsnummer", "RG-987"));
        card.setSummary("Напоминание об оплате счёта за электричество.");
        card.setRequiredAction(RequiredAction.PAY);
        card.setActionSteps(List.of("Оплатить 129.90 EUR до 15.06.2026", "Указать Kundennummer KD-12345"));
        card.setUrgency(Urgency.HIGH);
        card.setConfidence(Confidence.HIGH);
        card.setEvidenceQuotes(Map.of(
                "deadline", "Bitte zahlen Sie bis zum 15.06.2026",
                "amount", "Offener Betrag: 129,90 EUR",
                "required_action", "Wir bitten um umgehende Zahlung"));
        card.setRawAnalysis(Map.of("model", "test", "doc_type", "REMINDER_MAHNUNG"));

        DocumentCard saved = repository.saveAndFlush(card);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(CardStatus.NEW);

        DocumentCard found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOriginalFilename()).isEqualTo("mahnung.pdf");
        assertThat(found.getDocType()).isEqualTo(DocType.REMINDER_MAHNUNG);
        assertThat(found.getAmountValue()).isEqualByComparingTo("129.90");
        assertThat(found.getReferenceNumbers()).containsEntry("kundennummer", "KD-12345");
        assertThat(found.getActionSteps()).hasSize(2).first().asString().contains("129.90");
        assertThat(found.getEvidenceQuotes())
                .containsEntry("deadline", "Bitte zahlen Sie bis zum 15.06.2026")
                .containsKeys("amount", "required_action");
        assertThat(found.getRawAnalysis()).containsEntry("doc_type", "REMINDER_MAHNUNG");
    }
}
