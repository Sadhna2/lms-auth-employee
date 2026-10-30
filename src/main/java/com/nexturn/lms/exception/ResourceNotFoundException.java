package com.nexturn.lms.exception;

/** Signals a missing employee or manager. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
