package com.bureaucat.cards;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API representation of a document card. */
public record CardResponse(
        UUID id,
        OffsetDateTime createdAt,
        String originalFilename,
        SourceType sourceType,
        String language,
        DocType docType,
        String senderName,
        String senderCategory,
        LocalDate documentDate,
        LocalDate deadline,
        BigDecimal amountValue,
        String amountCurrency,
        Map<String, String> referenceNumbers,
        String summary,
        RequiredAction requiredAction,
        List<String> actionSteps,
        Urgency urgency,
        Confidence confidence,
        Map<String, String> evidenceQuotes,
        CardStatus status,
        String driveLink
) {

    public static CardResponse from(DocumentCard card) {
        return new CardResponse(
                card.getId(),
                card.getCreatedAt(),
                card.getOriginalFilename(),
                card.getSourceType(),
                card.getLanguage(),
                card.getDocType(),
                card.getSenderName(),
                card.getSenderCategory(),
                card.getDocumentDate(),
                card.getDeadline(),
                card.getAmountValue(),
                card.getAmountCurrency(),
                card.getReferenceNumbers(),
                card.getSummary(),
                card.getRequiredAction(),
                card.getActionSteps(),
                card.getUrgency(),
                card.getConfidence(),
                card.getEvidenceQuotes(),
                card.getStatus(),
                card.getDriveLink());
    }
}
