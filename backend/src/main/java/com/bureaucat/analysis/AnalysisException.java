package com.bureaucat.analysis;

public class AnalysisException extends RuntimeException {

    private final String rawResponse;

    public AnalysisException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
