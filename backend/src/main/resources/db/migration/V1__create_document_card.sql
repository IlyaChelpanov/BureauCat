CREATE TABLE document_card (
    id                UUID PRIMARY KEY,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    original_filename TEXT NOT NULL,
    source_type       TEXT NOT NULL CHECK (source_type IN ('PDF_TEXT', 'PDF_SCAN', 'IMAGE')),
    language          TEXT,
    doc_type          TEXT NOT NULL CHECK (doc_type IN (
                          'INVOICE', 'REMINDER_MAHNUNG', 'INSURANCE', 'TAX',
                          'BANK', 'RENTAL', 'EMPLOYMENT', 'MEDICAL', 'SCHOOL_KITA',
                          'GOVERNMENT', 'CONTRACT', 'INFO_ONLY', 'OTHER')),
    sender_name       TEXT,
    sender_category   TEXT,
    document_date     DATE,
    deadline          DATE,
    amount_value      NUMERIC,
    amount_currency   TEXT,
    reference_numbers JSONB,
    summary           TEXT,
    required_action   TEXT CHECK (required_action IN (
                          'PAY', 'RESPOND', 'SEND_DOCUMENTS', 'ATTEND', 'NONE_ARCHIVE')),
    action_steps      JSONB,
    urgency           TEXT CHECK (urgency IN ('HIGH', 'MEDIUM', 'LOW', 'NONE')),
    confidence        TEXT CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    evidence_quotes   JSONB,
    raw_analysis      JSONB,
    status            TEXT NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'IN_PROGRESS', 'DONE', 'ARCHIVED')),
    drive_file_id     TEXT,
    drive_link        TEXT,
    notion_page_id    TEXT,
    calendar_event_id TEXT
);

CREATE INDEX idx_document_card_created_at ON document_card (created_at DESC);
CREATE INDEX idx_document_card_deadline ON document_card (deadline) WHERE deadline IS NOT NULL;
CREATE INDEX idx_document_card_status ON document_card (status);
CREATE INDEX idx_document_card_doc_type ON document_card (doc_type);
