package com.nexturn.lms.dto;

import java.time.Instant;
import java.util.Map;

/** Stable error body shared by MVC validation, business errors and security failures. */
public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, Map<String, String> fieldErrors) { }
