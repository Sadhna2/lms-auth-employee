package com.nexturn.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credentials submitted for BCrypt verification; never log the password. */
public record LoginRequest(@NotBlank @Size(max = 100) String username,
                           @NotBlank @Size(max = 72) String password) {
    @Override public String toString() { return "LoginRequest[username=" + username + ", password=REDACTED]"; }
}
