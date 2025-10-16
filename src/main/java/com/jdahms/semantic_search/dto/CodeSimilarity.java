package com.jdahms.semantic_search.dto;

public interface CodeSimilarity {
    Long getId();
    String getCode();
    String getDescription();
    Double getSimilarity();
}
