package com.jdahms.semantic_search.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class OllamaApiException extends RuntimeException {

    public OllamaApiException(String message) {
        super(message);
    }

    public OllamaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
