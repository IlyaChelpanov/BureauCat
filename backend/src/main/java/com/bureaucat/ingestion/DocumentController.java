package com.bureaucat.ingestion;

import com.bureaucat.cards.DocumentCard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<DocumentCard> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!ingestionService.isSupported(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type: " + file.getContentType() + ". Allowed: PDF, JPG, PNG");
        }
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        DocumentCard card = ingestionService.ingest(filename, file.getContentType(), file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }
}
