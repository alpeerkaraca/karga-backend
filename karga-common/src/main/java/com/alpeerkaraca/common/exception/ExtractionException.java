package com.alpeerkaraca.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExtractionException extends RuntimeException {
    public ExtractionException(String message) {
        super(message);
    }
}
