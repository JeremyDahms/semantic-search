package com.jdahms.semantic_search.service;

import com.jdahms.semantic_search.entity.IndustryCode;
import com.jdahms.semantic_search.repository.CodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CodeBatchService {

    private final CodeRepository codeRepository;

    public CodeBatchService(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<IndustryCode> batch) {
        codeRepository.saveAll(batch);
    }
}
