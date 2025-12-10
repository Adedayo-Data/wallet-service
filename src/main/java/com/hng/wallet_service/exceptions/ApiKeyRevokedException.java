package com.hng.wallet_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ApiKeyRevokedException extends RuntimeException {
    public ApiKeyRevokedException(String message) {
        super(message);
    }
}
