package com.nexturn.lms.dto;

/** Minimal manager directory entry; excludes phone number, gender and home address. */
public record ManagerResponse(Integer empId, String empName, String email, String designation) { }
