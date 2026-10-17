package com.stockpilot.exception;

/**
 * The request is well-formed but breaks a business rule
 * (e.g. "not enough stock", "a RECEIVED order can't be cancelled").
 * Mapped to HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
