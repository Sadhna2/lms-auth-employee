package com.nexturn.lms.dto;

import com.nexturn.lms.enums.Role;
import jakarta.validation.constraints.*;

/** Account provisioning input. Username must match an existing employee email. */
public record CreateUserRequest(@NotBlank @Email @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 72) String password, @NotNull Role role) {
    @Override public String toString() { return "CreateUserRequest[username=" + username + ", password=REDACTED, role=" + role + "]"; }
}
