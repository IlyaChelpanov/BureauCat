package com.bureaucat.ingestion;

import com.bureaucat.analysis.AnalysisCost;
import com.bureaucat.analysis.AnalysisCostRepository;
import com.bureaucat.analysis.AnalysisOutcome;
import com.bureaucat.analysis.AnalysisResult;
import com.bureaucat.analysis.DocumentAnalyzer;
import com.bureaucat.cards.CardStatus;
import com.bureaucat.cards.DocumentCard;
import com.bureaucat.cards.DocumentCardRepository;
import com.bureaucat.cards.SourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;

@Service
public class IngestionService {

    private static final BigDecimal MEGA = BigDecimal.valueOf(1_000_000);

    private final SourceTypeDetector sourceTypeDetector;
    private final DocumentAnalyzer documentAnalyzer;
    private final DocumentCardRepository cardRepository;
    private final AnalysisCostRepository costRepository;
    private final BigDecimal inputCostPerMtok;
    private final BigDecimal outputCostPerMtok;

    public IngestionService(
            SourceTypeDetector sourceTypeDetector,
            DocumentAnalyzer documentAnalyzer,
            DocumentCardRepository cardRepository,
            AnalysisCostRepository costRepository,
            @Value("${anthropic.input-cost-per-mtok}") BigDecimal inputCostPerMtok,
            @Value("${anthropic.output-cost-per-mtok}") BigDecimal outputCostPerMtok) {
        this.sourceTypeDetector = sourceTypeDetector;
        this.documentAnalyzer = documentAnalyzer;
        this.cardRepository = cardRepository;
        this.costRepository = costRepository;
        this.inputCostPerMtok = inputCostPerMtok;
        this.outputCostPerMtok = outputCostPerMtok;
    }

    public boolean isSupported(String contentType) {
        return sourceTypeDetector.isSupported(contentType);
    }

    @Transactional
    public DocumentCard ingest(String originalFilename, String contentType, byte[] content) {
        SourceType sourceType = sourceTypeDetector.detect(contentType, content);
        AnalysisOutcome outcome = documentAnalyzer.analyze(sourceType, contentType, content);

        DocumentCard card = toCard(originalFilename, sourceType, outcome);
        card = cardRepository.save(card);

        AnalysisCost cost = new AnalysisCost();
        cost.setCardId(card.getId());
        cost.setModel(outcome.model());
        cost.setInputTokens(outcome.inputTokens());
        cost.setOutputTokens(outcome.outputTokens());
        cost.setCostUsd(costUsd(outcome.inputTokens(), outcome.outputTokens()));
        costRepository.save(cost);

        return card;
    }

    private BigDecimal costUsd(long inputTokens, long outputTokens) {
        BigDecimal input = inputCostPerMtok.multiply(BigDecimal.valueOf(inputTokens)).divide(MEGA, MathContext.DECIMAL64);
        BigDecimal output = outputCostPerMtok.multiply(BigDecimal.valueOf(outputTokens)).divide(MEGA, MathContext.DECIMAL64);
        return input.add(output).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private static DocumentCard toCard(String originalFilename, SourceType sourceType, AnalysisOutcome outcome) {
        AnalysisResult result = outcome.result();
        DocumentCard card = new DocumentCard();
        card.setOriginalFilename(originalFilename);
        card.setSourceType(sourceType);
        card.setLanguage(result.language());
        card.setDocType(result.docType());
        card.setSenderName(result.senderName());
        card.setSenderCategory(result.senderCategory());
        card.setDocumentDate(result.documentDate());
        card.setDeadline(result.deadline());
        card.setAmountValue(result.amountValue());
        card.setAmountCurrency(result.amountCurrency());
        card.setReferenceNumbers(result.referenceNumbers());
        card.setSummary(result.summary());
        card.setRequiredAction(result.requiredAction());
        card.setActionSteps(result.actionSteps());
        card.setUrgency(result.urgency());
        card.setConfidence(result.confidence());
        card.setEvidenceQuotes(result.evidenceQuotes());
        card.setRawAnalysis(outcome.rawAnalysis());
        card.setStatus(CardStatus.NEW);
        return card;
    }
}
