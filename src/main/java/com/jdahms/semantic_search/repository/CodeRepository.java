package com.jdahms.semantic_search.repository;

import com.jdahms.semantic_search.dto.CodeSimilarity;
import com.jdahms.semantic_search.entity.IndustryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodeRepository extends JpaRepository<IndustryCode, Long> {

    @Query(value = """
        SELECT id, code, description, embedding,
                (1 - (embedding <=> cast(:queryEmbedding as vector))) as similarity
        FROM industry_codes
        WHERE embedding IS NOT NULL
        ORDER BY embedding <=> cast(:queryEmbedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CodeSimilarity> findSimilarCodes(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);

    Optional<IndustryCode> findByCode(String code);
}
