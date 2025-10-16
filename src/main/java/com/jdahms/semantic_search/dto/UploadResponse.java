package com.jdahms.semantic_search.dto;

public record UploadResponse(
        int totalProcessed,
        int successful,
        int failed,
        String message
) {
}
