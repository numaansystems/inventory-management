package com.numaansystems.inventory.common;

/**
 * Thrown when a request is well-formed but conflicts with the current state of the system
 * (duplicate SKU, insufficient stock, ...). Mapped to HTTP 409 with {@link #code()} exposed to clients.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
