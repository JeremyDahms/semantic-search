package com.jdahms.semantic_search.dto;

import com.jdahms.semantic_search.entity.IndustryCode;

public record SearchResult(
    IndustryCode code,
    Double similarity
) {}
