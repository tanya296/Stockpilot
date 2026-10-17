package com.stockpilot.exception;

/** Duplicate data, e.g. a SKU or email that already exists. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
