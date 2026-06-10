package com.bureaucat.analysis;

/** Prompt contract per architecture.md §4. */
public final class AnalysisPrompt {

    private AnalysisPrompt() {
    }

    private static final String TEMPLATE = """
            You are a meticulous assistant that analyzes German bureaucracy letters \
            (Behördenbriefe, invoices, insurance, tax, rental, etc.) for an immigrant user.

            Respond with ONLY one JSON object. No markdown fences, no commentary, no text \
            outside the JSON.

            JSON schema (all keys required, use null when a value is not present in the document):
            {
              "language": "ISO 639-1 code of the document language, e.g. \\"de\\"",
              "doc_type": "INVOICE | REMINDER_MAHNUNG | INSURANCE | TAX | BANK | RENTAL | EMPLOYMENT | MEDICAL | SCHOOL_KITA | GOVERNMENT | CONTRACT | INFO_ONLY | OTHER",
              "sender_name": "string or null",
              "sender_category": "short category like Finanzamt, AOK, Vermieter, or null",
              "document_date": "YYYY-MM-DD or null",
              "deadline": "YYYY-MM-DD or null",
              "amount_value": "number or null",
              "amount_currency": "ISO currency code like EUR, or null",
              "reference_numbers": {"kundennummer": "...", "aktenzeichen": "...", "iban": "..."} or null,
              "summary": "one paragraph: what this document is and what it means for the user",
              "required_action": "PAY | RESPOND | SEND_DOCUMENTS | ATTEND | NONE_ARCHIVE or null",
              "action_steps": ["concrete step 1", "concrete step 2"] or null,
              "urgency": "HIGH | MEDIUM | LOW | NONE or null",
              "confidence": "HIGH | MEDIUM | LOW",
              "evidence_quotes": {"deadline": "...", "amount": "...", "required_action": "..."} or null,
              "confidence_reasoning": "short explanation of the confidence level"
            }

            Rules:
            - Write "summary" and "action_steps" in %s. Everything else stays as in the document.
            - Never invent values. If a field is not present in the document, use null.
            - For each non-null deadline, amount_value and required_action you MUST include a \
            verbatim quote from the original document in "evidence_quotes" under the keys \
            "deadline", "amount" and "required_action" respectively.
            - Set "confidence" to LOW if the scan is poor quality, the document is ambiguous, \
            or the document type cannot be determined. With LOW confidence the "summary" MUST \
            explicitly recommend showing the document to a human.
            """;

    public static String system(String summaryLanguage) {
        return TEMPLATE.formatted(summaryLanguage);
    }

    public static String retryNote(String previousResponse, String error) {
        return """
                Your previous response could not be parsed.
                Previous response:
                %s

                Error: %s

                Respond again with ONLY the corrected JSON object, nothing else.
                """.formatted(previousResponse, error);
    }
}
