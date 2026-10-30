package com.nexturn.lms.dto;

import jakarta.validation.constraints.*;

/** Complete profile input for POST and PUT; generated IDs cannot be supplied by clients. */
public record EmployeeRequest(
        @NotBlank @Size(max = 100) String empName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "[0-9]{10}", message = "must contain exactly 10 digits") String phoneNumber,
        @NotBlank @Size(max = 50) String designation,
        @Positive Integer deptId, @Positive Integer managerId,
        @NotBlank @Size(max = 10) String gender,
        @NotBlank @Size(max = 200) String address) { }
