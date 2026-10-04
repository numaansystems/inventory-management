package com.numaansystems.inventory.common;

/**
 * Thrown for field-level validation that depends on other fields and so cannot be expressed with Bean
 * Validation annotations. Rendered exactly like annotation-driven validation failures (HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
