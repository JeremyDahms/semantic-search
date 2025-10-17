package com.jdahms.semantic_search.dto;

import com.jdahms.semantic_search.entity.IndustryCode;

public record CodeResponse(
        Long id,
        String code,
        String description
) {
    public static CodeResponse from(IndustryCode entity) {
        return new CodeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getDescription()
        );
    }
}
