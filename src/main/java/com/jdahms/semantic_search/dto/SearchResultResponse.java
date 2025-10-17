package com.jdahms.semantic_search.dto;

public record SearchResultResponse(
        Long id,
        String code,
        String description,
        Double similarity
) {
}
