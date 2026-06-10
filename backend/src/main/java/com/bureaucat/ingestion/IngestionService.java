package com.bureaucat.ingestion;

import com.bureaucat.cards.SourceType;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private final SourceTypeDetector sourceTypeDetector;

    public IngestionService(SourceTypeDetector sourceTypeDetector) {
        this.sourceTypeDetector = sourceTypeDetector;
    }

    public SourceType detectSourceType(String contentType, byte[] content) {
        return sourceTypeDetector.detect(contentType, content);
    }

    public boolean isSupported(String contentType) {
        return sourceTypeDetector.isSupported(contentType);
    }
}
