package com.nexturn.lms.dto;

/** Safe account result with a user-facing success message; never contains credentials or tokens. */
public record LoginResponse(String username,String role,String message) {
    public LoginResponse(String username,String role) { this(username,role,"Login successful."); }
}
