package com.bureaucat.analysis;

import com.anthropic.models.messages.ContentBlockParam;

import java.util.List;

/** Thin seam over the Anthropic API so the analyzer logic is unit-testable. */
public interface AnthropicMessenger {

    String complete(String systemPrompt, List<ContentBlockParam> userContent);
}
