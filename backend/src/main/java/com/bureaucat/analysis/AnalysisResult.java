package com.bureaucat.analysis;

import com.bureaucat.cards.Confidence;
import com.bureaucat.cards.DocType;
import com.bureaucat.cards.RequiredAction;
import com.bureaucat.cards.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Strict JSON contract the LLM must return. Field names map from snake_case. */
public record AnalysisResult(
        String language,
        @NotNull DocType docType,
        String senderName,
        String senderCategory,
        LocalDate documentDate,
        LocalDate deadline,
        BigDecimal amountValue,
        String amountCurrency,
        Map<String, String> referenceNumbers,
        @NotBlank String summary,
        RequiredAction requiredAction,
        List<String> actionSteps,
        Urgency urgency,
        @NotNull Confidence confidence,
        Map<String, String> evidenceQuotes,
        String confidenceReasoning
) {
}
