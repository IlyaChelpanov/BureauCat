package com.bureaucat.analysis;

import com.bureaucat.cards.SourceType;

public interface DocumentAnalyzer {

    /**
     * @throws AnalysisException when the model fails to produce a valid result after retry;
     *                           the raw model response is preserved for debugging
     */
    AnalysisResult analyze(SourceType sourceType, String contentType, byte[] content);
}
