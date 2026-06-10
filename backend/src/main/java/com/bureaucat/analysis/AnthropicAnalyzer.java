package com.bureaucat.analysis;

import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.TextBlockParam;
import com.bureaucat.cards.SourceType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnthropicAnalyzer implements DocumentAnalyzer {

    private static final int MAX_SCAN_PAGES = 8;
    private static final int RENDER_DPI = 150;

    private final AnthropicMessenger messenger;
    private final Validator validator;
    private final String summaryLanguage;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AnthropicAnalyzer(
            AnthropicMessenger messenger,
            Validator validator,
            @Value("${anthropic.summary-language}") String summaryLanguage) {
        this.messenger = messenger;
        this.validator = validator;
        this.summaryLanguage = summaryLanguage;
    }

    @Override
    public AnalysisResult analyze(SourceType sourceType, String contentType, byte[] content) {
        String system = AnalysisPrompt.system(summaryLanguage);
        List<ContentBlockParam> userContent = buildUserContent(sourceType, contentType, content);

        String response = messenger.complete(system, userContent);
        try {
            return parseAndValidate(response);
        } catch (Exception firstError) {
            List<ContentBlockParam> retryContent = new ArrayList<>(userContent);
            retryContent.add(text(AnalysisPrompt.retryNote(response, firstError.getMessage())));
            String retryResponse = messenger.complete(system, retryContent);
            try {
                return parseAndValidate(retryResponse);
            } catch (Exception retryError) {
                throw new AnalysisException(
                        "Model returned invalid analysis result after retry: " + retryError.getMessage(),
                        retryResponse, retryError);
            }
        }
    }

    private AnalysisResult parseAndValidate(String response) throws IOException {
        AnalysisResult result = objectMapper.readValue(stripFences(response), AnalysisResult.class);
        var violations = validator.validate(result);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Analysis result failed validation: " + message);
        }
        return result;
    }

    /** Defensive: the prompt forbids markdown fences, but strip them if the model adds any. */
    private static String stripFences(String response) {
        String trimmed = response.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(trimmed.indexOf('\n') + 1);
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd);
            }
        }
        return trimmed.strip();
    }

    private List<ContentBlockParam> buildUserContent(SourceType sourceType, String contentType, byte[] content) {
        return switch (sourceType) {
            case PDF_TEXT -> List.of(text("Analyze this letter:\n\n" + extractPdfText(content)));
            case PDF_SCAN -> scanContent(content);
            case IMAGE -> List.of(
                    image(contentType, content),
                    text("Analyze the letter on this photo."));
        };
    }

    private List<ContentBlockParam> scanContent(byte[] pdf) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), MAX_SCAN_PAGES);
            for (int i = 0; i < pages; i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, RENDER_DPI);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(pageImage, "png", out);
                blocks.add(image("image/png", out.toByteArray()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render PDF pages", e);
        }
        blocks.add(text("Analyze the scanned letter on these page images."));
        return blocks;
    }

    private String extractPdfText(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract PDF text", e);
        }
    }

    private static ContentBlockParam text(String text) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(text).build());
    }

    private static ContentBlockParam image(String contentType, byte[] data) {
        Base64ImageSource.MediaType mediaType = switch (contentType) {
            case "image/jpeg" -> Base64ImageSource.MediaType.IMAGE_JPEG;
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            default -> throw new IllegalArgumentException("Unsupported image content type: " + contentType);
        };
        return ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(mediaType)
                        .data(Base64.getEncoder().encodeToString(data))
                        .build())
                .build());
    }
}
