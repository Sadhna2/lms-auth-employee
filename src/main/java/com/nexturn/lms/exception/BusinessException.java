package com.nexturn.lms.exception;

/** Signals invalid manager relationships or other business rules. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
