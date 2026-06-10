package com.bureaucat.analysis;

import com.anthropic.models.messages.ContentBlockParam;
import com.bureaucat.cards.Confidence;
import com.bureaucat.cards.DocType;
import com.bureaucat.cards.RequiredAction;
import com.bureaucat.cards.SourceType;
import com.bureaucat.ingestion.PdfFixtures;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicAnalyzerTest {

    private static final String VALID_RESPONSE = """
            {
              "language": "de",
              "doc_type": "REMINDER_MAHNUNG",
              "sender_name": "Stadtwerke München",
              "sender_category": "Versorger",
              "document_date": "2026-05-20",
              "deadline": "2026-06-15",
              "amount_value": 129.90,
              "amount_currency": "EUR",
              "reference_numbers": {"kundennummer": "KD-12345"},
              "summary": "Напоминание об оплате счёта за электричество.",
              "required_action": "PAY",
              "action_steps": ["Оплатить 129.90 EUR до 15.06.2026"],
              "urgency": "HIGH",
              "confidence": "HIGH",
              "evidence_quotes": {
                "deadline": "Bitte zahlen Sie bis zum 15.06.2026",
                "amount": "Offener Betrag: 129,90 EUR",
                "required_action": "Wir bitten um umgehende Zahlung"
              },
              "confidence_reasoning": "Clear text layer, unambiguous Mahnung."
            }
            """;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /** Records calls and returns queued responses. */
    private static class FakeMessenger implements AnthropicMessenger {
        final Deque<String> responses = new ArrayDeque<>();
        final List<List<ContentBlockParam>> calls = new ArrayList<>();

        @Override
        public ModelCompletion complete(String systemPrompt, List<ContentBlockParam> userContent) {
            calls.add(userContent);
            return new ModelCompletion(responses.pop(), "test-model", 1000, 200);
        }
    }

    private final FakeMessenger messenger = new FakeMessenger();
    private final AnthropicAnalyzer analyzer = new AnthropicAnalyzer(messenger, validator, "Russian");

    @Test
    void parsesValidResponse() throws Exception {
        messenger.responses.add(VALID_RESPONSE);
        byte[] pdf = PdfFixtures.pdfWithText("Mahnung: Bitte zahlen Sie 129,90 EUR bis zum 15.06.2026. Kundennummer KD-12345.");

        AnalysisOutcome outcome = analyzer.analyze(SourceType.PDF_TEXT, "application/pdf", pdf);

        AnalysisResult result = outcome.result();
        assertThat(result.docType()).isEqualTo(DocType.REMINDER_MAHNUNG);
        assertThat(result.deadline()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(result.amountValue()).isEqualByComparingTo(new BigDecimal("129.90"));
        assertThat(result.requiredAction()).isEqualTo(RequiredAction.PAY);
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.evidenceQuotes()).containsKey("deadline");
        assertThat(outcome.model()).isEqualTo("test-model");
        assertThat(outcome.inputTokens()).isEqualTo(1000);
        assertThat(outcome.outputTokens()).isEqualTo(200);
        assertThat(outcome.rawAnalysis()).containsEntry("doc_type", "REMINDER_MAHNUNG");
        assertThat(messenger.calls).hasSize(1);
    }

    @Test
    void parsesValidResponseWrappedInMarkdownFences() throws Exception {
        messenger.responses.add("```json\n" + VALID_RESPONSE + "\n```");
        byte[] pdf = PdfFixtures.pdfWithText("Mahnung über 129,90 EUR, zahlbar bis 15.06.2026, Kundennummer KD-12345.");

        AnalysisOutcome outcome = analyzer.analyze(SourceType.PDF_TEXT, "application/pdf", pdf);

        assertThat(outcome.result().docType()).isEqualTo(DocType.REMINDER_MAHNUNG);
    }

    @Test
    void invalidJsonRetriesOnceThenSucceeds() throws Exception {
        messenger.responses.add("Sorry, here is the analysis: not json");
        messenger.responses.add(VALID_RESPONSE);
        byte[] pdf = PdfFixtures.pdfWithText("Mahnung über 129,90 EUR, zahlbar bis 15.06.2026, Kundennummer KD-12345.");

        AnalysisOutcome outcome = analyzer.analyze(SourceType.PDF_TEXT, "application/pdf", pdf);

        assertThat(outcome.result().confidence()).isEqualTo(Confidence.HIGH);
        assertThat(messenger.calls).hasSize(2);
        // retry call carries the original content plus an error note
        assertThat(messenger.calls.get(1)).hasSize(messenger.calls.get(0).size() + 1);
        // usage accumulates across both calls
        assertThat(outcome.inputTokens()).isEqualTo(2000);
        assertThat(outcome.outputTokens()).isEqualTo(400);
    }

    @Test
    void invalidJsonTwiceFailsWithRawResponse() throws Exception {
        messenger.responses.add("not json at all");
        messenger.responses.add("still not json");
        byte[] pdf = PdfFixtures.pdfWithText("Mahnung über 129,90 EUR, zahlbar bis 15.06.2026, Kundennummer KD-12345.");

        assertThatThrownBy(() -> analyzer.analyze(SourceType.PDF_TEXT, "application/pdf", pdf))
                .isInstanceOf(AnalysisException.class)
                .satisfies(e -> assertThat(((AnalysisException) e).getRawResponse()).isEqualTo("still not json"));
        assertThat(messenger.calls).hasSize(2);
    }

    @Test
    void validJsonMissingRequiredFieldsFailsValidationAndRetries() throws Exception {
        messenger.responses.add("{\"language\": \"de\"}");
        messenger.responses.add(VALID_RESPONSE);
        byte[] pdf = PdfFixtures.pdfWithText("Mahnung über 129,90 EUR, zahlbar bis 15.06.2026, Kundennummer KD-12345.");

        AnalysisOutcome outcome = analyzer.analyze(SourceType.PDF_TEXT, "application/pdf", pdf);

        assertThat(outcome.result().summary()).isNotBlank();
        assertThat(messenger.calls).hasSize(2);
    }

    @Test
    void scannedPdfSendsOneImageBlockPerPagePlusInstruction() throws Exception {
        messenger.responses.add(VALID_RESPONSE);
        byte[] pdf = PdfFixtures.pdfWithoutText();

        analyzer.analyze(SourceType.PDF_SCAN, "application/pdf", pdf);

        List<ContentBlockParam> content = messenger.calls.get(0);
        assertThat(content).hasSize(2); // 1 page image + instruction text
        assertThat(content.get(0).isImage()).isTrue();
        assertThat(content.get(1).isText()).isTrue();
    }
}
