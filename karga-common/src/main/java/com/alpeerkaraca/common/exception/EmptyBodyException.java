package com.alpeerkaraca.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyBodyException extends RuntimeException {
    public EmptyBodyException(String message) {
        super(message);
    }
}
