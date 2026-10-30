package com.nexturn.lms.exception;

/** Signals duplicate data or a change blocked by existing relationships. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
