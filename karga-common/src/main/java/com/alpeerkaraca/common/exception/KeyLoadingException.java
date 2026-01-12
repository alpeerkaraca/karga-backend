package com.alpeerkaraca.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KeyLoadingException extends RuntimeException {
    public KeyLoadingException(String message) {
        super(message);
    }

    public KeyLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}

