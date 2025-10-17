package com.jdahms.semantic_search.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodeNotFoundException extends RuntimeException {
    public CodeNotFoundException(Long id) {
        super("Code not found with id: " + id);
    }

    public CodeNotFoundException(String code) {
        super("Code not found with code: " + code);
    }
}
