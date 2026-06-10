CREATE TABLE analysis_cost (
    id            UUID PRIMARY KEY,
    card_id       UUID NOT NULL REFERENCES document_card (id),
    model         TEXT NOT NULL,
    input_tokens  BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    cost_usd      NUMERIC(12, 6) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analysis_cost_card_id ON analysis_cost (card_id);
