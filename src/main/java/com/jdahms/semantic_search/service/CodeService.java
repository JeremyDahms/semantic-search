package com.jdahms.semantic_search.service;

import com.jdahms.semantic_search.entity.IndustryCode;
import com.jdahms.semantic_search.repository.CodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodeService {

    private final CodeRepository codeRepository;
    private final OllamaService ollamaService;

    public CodeService(CodeRepository codeRepository, OllamaService ollamaService) {
        this.codeRepository = codeRepository;
        this.ollamaService = ollamaService;
    }

    @Transactional
    public int uploadCodesFromCsv(MultipartFile file) throws Exception {
        List<IndustryCode> codes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                throw new IllegalArgumentException("File is empty");
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if(line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] parts = line.split(",", 2);

                if (parts.length != 2) {
                    System.err.println("Skipping malformed line " + lineNumber + ": " + line);
                    continue;
                }

                String code = parts[0].trim();
                String description = parts[1].trim();

                // Create entity
                IndustryCode industryCode = new IndustryCode(code, description);

                // Generate embedding
                float[] embedding = ollamaService.generateEmbedding(description);
                industryCode.setEmbedding(embedding);

                codes.add(industryCode);

                System.out.println("Processed: " + code + " (" + codes.size() + " total)");
            }
        }

        // Batch save all codes
        List<IndustryCode> savedCodes = codeRepository.saveAll(codes);
        return savedCodes.size();
    }
}
