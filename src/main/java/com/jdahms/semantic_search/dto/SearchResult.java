package com.jdahms.semantic_search.dto;

public record SearchResult(
        Long id,
        String code,
        String description,
        Double similarity
) {
}
