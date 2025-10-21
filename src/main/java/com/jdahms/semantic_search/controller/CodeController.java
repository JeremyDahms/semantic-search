package com.jdahms.semantic_search.controller;

import com.jdahms.semantic_search.dto.CodeResponse;
import com.jdahms.semantic_search.dto.CreateCodeRequest;
import com.jdahms.semantic_search.dto.CsvUploadResult;
import com.jdahms.semantic_search.dto.SearchResult;
import com.jdahms.semantic_search.dto.UpdateCodeRequest;
import com.jdahms.semantic_search.dto.UploadResponse;
import com.jdahms.semantic_search.service.CodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/codes")
@Validated
public class CodeController {

    private final CodeService codeService;

    public CodeController(CodeService codeService) {
        this.codeService = codeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<CodeResponse> uploadCode(@Valid @RequestBody CreateCodeRequest request) {
        CodeResponse response = codeService.createCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<UploadResponse> uploadCsv(@RequestParam("file") MultipartFile file) {
        CsvUploadResult result = codeService.uploadCodesFromCsv(file);
        String message = String.format("CSV upload completed: %d successful, %d failed",
                result.successful(), result.failed());
        UploadResponse response = new UploadResponse(
                result.getTotalProcessed(),
                result.successful(),
                result.failed(),
                message
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CodeResponse>> getAllCodes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // Default sort by ID for consistent pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok(codeService.getAllCodes(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @RequestParam @NotBlank @Size(min = 1, max = 500) String query,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(codeService.searchCodes(query, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeResponse> getCodeById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(codeService.getCodeById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<CodeResponse> getCodeByCode(
            @PathVariable @NotBlank @Size(max = 50) String code) {
        return ResponseEntity.ok(codeService.getCodeByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CodeResponse> updateCode(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateCodeRequest request) {
        return ResponseEntity.ok(codeService.updateCode(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCode(@PathVariable @Min(1) Long id) {
        codeService.deleteCode(id);
        return ResponseEntity.noContent().build();
    }
}
