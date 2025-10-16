package com.jdahms.semantic_search.controller;

import com.jdahms.semantic_search.dto.CodeSimilarity;
import com.jdahms.semantic_search.dto.SearchResult;
import com.jdahms.semantic_search.dto.UploadResponse;
import com.jdahms.semantic_search.entity.IndustryCode;
import com.jdahms.semantic_search.repository.CodeRepository;
import com.jdahms.semantic_search.service.CodeService;
import com.jdahms.semantic_search.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
public class CodeController {

    private final CodeRepository repository;
    private final OllamaService ollamaService;
    private final CodeService codeService;

    public CodeController(CodeRepository repository, OllamaService ollamaService, CodeService codeService) {
        this.repository = repository;
        this.ollamaService = ollamaService;
        this.codeService = codeService;
    }

    @PostMapping("/upload")
    public IndustryCode uploadCodes(@RequestBody CodeRequest request) {
        IndustryCode code = new IndustryCode(request.code(), request.description());

        float[] embedding = ollamaService.generateEmbedding(code.getDescription());
        code.setEmbedding(embedding);

        return repository.save(code);
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<UploadResponse> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new UploadResponse(0,0,0, "File is empty"));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body(new UploadResponse(0,0,0, "File must be a CSV"));
            }

            int count = codeService.uploadCodesFromCsv(file);

            return ResponseEntity.ok(new UploadResponse(count, count,0, "Successfully uploaded " + count + " codes"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new UploadResponse(0,0,0, "Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public List<IndustryCode> getAllCodes() {
        return repository.findAll();
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam String query, @RequestParam(defaultValue = "5") int limit) {
        float[] queryEmbedding = ollamaService.generateEmbedding(query);
        String embeddingStr = ollamaService.embeddingToString(queryEmbedding);

        List<CodeSimilarity> results = repository.findSimilarCodes(embeddingStr, limit);

        return results.stream()
                .map(result -> {
                    IndustryCode code = new IndustryCode();
                    code.setId(result.getId());
                    code.setCode(result.getCode());
                    code.setDescription(result.getDescription());

                    return new SearchResult(code, result.getSimilarity());
                }).toList();
    }

    public record CodeRequest(String code, String description) {}
}
