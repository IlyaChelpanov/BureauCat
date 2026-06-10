package com.bureaucat.analysis;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnthropicSdkMessenger implements AnthropicMessenger {

    private final String apiKey;
    private final String model;
    private volatile AnthropicClient client;

    public AnthropicSdkMessenger(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ModelCompletion complete(String systemPrompt, List<ContentBlockParam> userContent) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(8192L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .system(systemPrompt)
                .addUserMessageOfBlockParams(userContent)
                .build();
        Message response = client().messages().create(params);
        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
        return new ModelCompletion(text, model,
                response.usage().inputTokens(), response.usage().outputTokens());
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException("anthropic.api-key is not configured (set ANTHROPIC_API_KEY)");
                    }
                    client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                }
            }
        }
        return client;
    }
}
