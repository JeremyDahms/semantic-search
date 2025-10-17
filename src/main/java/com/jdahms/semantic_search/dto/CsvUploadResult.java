package com.jdahms.semantic_search.dto;

public record CsvUploadResult(
        int successful,
        int failed
) {
    public int getTotalProcessed() {
        return successful + failed;
    }
}
