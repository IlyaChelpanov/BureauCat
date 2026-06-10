package com.bureaucat.analysis;

import java.util.Map;

/** Successful analysis plus the metadata needed for persistence and cost tracking. */
public record AnalysisOutcome(
        AnalysisResult result,
        Map<String, Object> rawAnalysis,
        String model,
        long inputTokens,
        long outputTokens
) {
}
