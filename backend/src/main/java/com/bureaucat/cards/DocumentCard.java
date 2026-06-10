package com.bureaucat.cards;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_card")
public class DocumentCard {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocType docType;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_category")
    private String senderCategory;

    @Column(name = "document_date")
    private LocalDate documentDate;

    private LocalDate deadline;

    @Column(name = "amount_value")
    private BigDecimal amountValue;

    @Column(name = "amount_currency")
    private String amountCurrency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reference_numbers")
    private Map<String, String> referenceNumbers;

    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_action")
    private RequiredAction requiredAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_steps")
    private List<String> actionSteps;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    private Confidence confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_quotes")
    private Map<String, String> evidenceQuotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_analysis")
    private Map<String, Object> rawAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.NEW;

    @Column(name = "drive_file_id")
    private String driveFileId;

    @Column(name = "drive_link")
    private String driveLink;

    @Column(name = "notion_page_id")
    private String notionPageId;

    @Column(name = "calendar_event_id")
    private String calendarEventId;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderCategory() {
        return senderCategory;
    }

    public void setSenderCategory(String senderCategory) {
        this.senderCategory = senderCategory;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getAmountValue() {
        return amountValue;
    }

    public void setAmountValue(BigDecimal amountValue) {
        this.amountValue = amountValue;
    }

    public String getAmountCurrency() {
        return amountCurrency;
    }

    public void setAmountCurrency(String amountCurrency) {
        this.amountCurrency = amountCurrency;
    }

    public Map<String, String> getReferenceNumbers() {
        return referenceNumbers;
    }

    public void setReferenceNumbers(Map<String, String> referenceNumbers) {
        this.referenceNumbers = referenceNumbers;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public RequiredAction getRequiredAction() {
        return requiredAction;
    }

    public void setRequiredAction(RequiredAction requiredAction) {
        this.requiredAction = requiredAction;
    }

    public List<String> getActionSteps() {
        return actionSteps;
    }

    public void setActionSteps(List<String> actionSteps) {
        this.actionSteps = actionSteps;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public Map<String, String> getEvidenceQuotes() {
        return evidenceQuotes;
    }

    public void setEvidenceQuotes(Map<String, String> evidenceQuotes) {
        this.evidenceQuotes = evidenceQuotes;
    }

    public Map<String, Object> getRawAnalysis() {
        return rawAnalysis;
    }

    public void setRawAnalysis(Map<String, Object> rawAnalysis) {
        this.rawAnalysis = rawAnalysis;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public String getDriveFileId() {
        return driveFileId;
    }

    public void setDriveFileId(String driveFileId) {
        this.driveFileId = driveFileId;
    }

    public String getDriveLink() {
        return driveLink;
    }

    public void setDriveLink(String driveLink) {
        this.driveLink = driveLink;
    }

    public String getNotionPageId() {
        return notionPageId;
    }

    public void setNotionPageId(String notionPageId) {
        this.notionPageId = notionPageId;
    }

    public String getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(String calendarEventId) {
        this.calendarEventId = calendarEventId;
    }
}
