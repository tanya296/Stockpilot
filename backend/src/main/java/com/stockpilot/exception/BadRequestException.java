package com.stockpilot.exception;

/** The client sent something invalid that Bean Validation can't catch (e.g. an unknown sort field). Mapped to 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
