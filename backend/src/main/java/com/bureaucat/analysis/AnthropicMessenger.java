package com.bureaucat.analysis;

import com.anthropic.models.messages.ContentBlockParam;

import java.util.List;

/** Thin seam over the Anthropic API so the analyzer logic is unit-testable. */
public interface AnthropicMessenger {

    ModelCompletion complete(String systemPrompt, List<ContentBlockParam> userContent);

    record ModelCompletion(String text, String model, long inputTokens, long outputTokens) {
    }
}
