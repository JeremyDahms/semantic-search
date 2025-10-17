package com.jdahms.semantic_search.service;

import com.jdahms.semantic_search.constant.VectorConstants;
import com.jdahms.semantic_search.dto.CodeResponse;
import com.jdahms.semantic_search.dto.CodeSimilarity;
import com.jdahms.semantic_search.dto.CreateCodeRequest;
import com.jdahms.semantic_search.dto.CsvUploadResult;
import com.jdahms.semantic_search.dto.SearchResultResponse;
import com.jdahms.semantic_search.dto.UpdateCodeRequest;
import com.jdahms.semantic_search.entity.IndustryCode;
import com.jdahms.semantic_search.exception.CodeNotFoundException;
import com.jdahms.semantic_search.repository.CodeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CodeService {

    private static final Logger logger = LoggerFactory.getLogger(CodeService.class);

    private final CodeRepository codeRepository;
    private final OllamaService ollamaService;

    public CodeService(CodeRepository codeRepository, OllamaService ollamaService) {
        this.codeRepository = codeRepository;
        this.ollamaService = ollamaService;
    }

    @Transactional
    public CodeResponse createCode(CreateCodeRequest request) {
        IndustryCode code = new IndustryCode(request.code(), request.description());

        // Generate embedding
        float[] embedding = ollamaService.generateEmbedding(request.description());
        code.setEmbedding(embedding);

        IndustryCode savedCode = codeRepository.save(code);
        return CodeResponse.from(savedCode);
    }

    @Transactional(readOnly = true)
    public CodeResponse getCodeById(Long id) {
        IndustryCode code = codeRepository.findById(id)
                .orElseThrow(() -> new CodeNotFoundException(id));
        return CodeResponse.from(code);
    }

    @Transactional(readOnly = true)
    public CodeResponse getCodeByCode(String code) {
        IndustryCode industryCode = codeRepository.findByCode(code)
                .orElseThrow(() -> new CodeNotFoundException(code));
        return CodeResponse.from(industryCode);
    }

    @Transactional(readOnly = true)
    public Page<CodeResponse> getAllCodes(Pageable pageable) {
        return codeRepository.findAll(pageable)
                .map(CodeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SearchResultResponse> searchCodes(String query, int limit) {
        // Generate query embedding
        float[] queryEmbedding = ollamaService.generateEmbedding(query);
        String embeddingStr = ollamaService.embeddingToString(queryEmbedding);

        // Search using vector similarity
        List<CodeSimilarity> results = codeRepository.findSimilarCodes(embeddingStr, limit);

        // Map to response DTO
        return results.stream()
                .map(result -> new SearchResultResponse(
                        result.id(),
                        result.code(),
                        result.description(),
                        result.similarity()
                ))
                .toList();
    }

    @Transactional
    public CodeResponse updateCode(Long id, UpdateCodeRequest request) {
        IndustryCode code = codeRepository.findById(id)
                .orElseThrow(() -> new CodeNotFoundException(id));

        // Use Objects.equals() for null-safe comparison
        boolean descriptionChanged = !Objects.equals(code.getDescription(), request.description());

        // Update fields
        code.setCode(request.code());
        code.setDescription(request.description());

        // Regenerate embedding only if description changed
        if (descriptionChanged) {
            float[] embedding = ollamaService.generateEmbedding(request.description());
            code.setEmbedding(embedding);
        }

        IndustryCode updatedCode = codeRepository.save(code);
        return CodeResponse.from(updatedCode);
    }

    @Transactional
    public void deleteCode(Long id) {
        IndustryCode code = codeRepository.findById(id)
                .orElseThrow(() -> new CodeNotFoundException(id));
        codeRepository.delete(code);
    }

    public CsvUploadResult uploadCodesFromCsv(MultipartFile file) throws Exception {
        // Validation
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV");
        }

        int totalProcessed = 0;
        int totalFailed = 0;
        List<IndustryCode> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Use Apache Commons CSV for proper parsing
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            try (CSVParser csvParser = csvFormat.parse(reader)) {
                int rowCount = 0;

                for (CSVRecord record : csvParser) {
                    rowCount++;

                    // Enforce row limit
                    if (rowCount > VectorConstants.MAX_CSV_ROWS) {
                        logger.warn("CSV upload exceeded maximum row limit of {}. Stopping processing.",
                                VectorConstants.MAX_CSV_ROWS);
                        throw new IllegalArgumentException(
                                String.format("CSV file exceeds maximum allowed rows (%d). Only first %d rows will be processed.",
                                        VectorConstants.MAX_CSV_ROWS, VectorConstants.MAX_CSV_ROWS));
                    }

                    try {
                        // Expect columns: code, description
                        if (record.size() < 2) {
                            logger.warn("Skipping malformed row {} (insufficient columns): {}",
                                    record.getRecordNumber(), record);
                            totalFailed++;
                            continue;
                        }

                        String code = record.get(0);
                        String description = record.get(1);

                        if (code.isBlank() || description.isBlank()) {
                            logger.warn("Skipping row {} with blank fields", record.getRecordNumber());
                            totalFailed++;
                            continue;
                        }

                        // Create entity
                        IndustryCode industryCode = new IndustryCode(code, description);

                        // Generate embedding
                        float[] embedding = ollamaService.generateEmbedding(description);
                        industryCode.setEmbedding(embedding);

                        batch.add(industryCode);
                        totalProcessed++;

                        // Process batch when it reaches batch size
                        if (batch.size() >= VectorConstants.CSV_BATCH_SIZE) {
                            saveBatch(batch);
                            batch.clear();

                            // Add delay to prevent overwhelming Ollama service
                            Thread.sleep(VectorConstants.OLLAMA_BATCH_DELAY_MS);

                            logger.info("Processed {} codes so far ({} failed)...", totalProcessed, totalFailed);
                        }

                    } catch (Exception e) {
                        logger.error("Error processing row {}: {}", record.getRecordNumber(), e.getMessage());
                        totalFailed++;
                        // Continue processing other rows
                    }
                }

                // Save remaining batch
                if (!batch.isEmpty()) {
                    saveBatch(batch);
                    batch.clear();
                }

                logger.info("CSV upload completed: {} codes processed successfully, {} failed",
                        totalProcessed, totalFailed);
            }
        }

        return new CsvUploadResult(totalProcessed, totalFailed);
    }

    /**
     * Save a batch of codes in a separate transaction.
     * This allows partial success if some batches fail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveBatch(List<IndustryCode> batch) {
        codeRepository.saveAll(batch);
    }
}
