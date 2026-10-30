package com.nexturn.lms.config;

import com.nexturn.lms.repository.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves ownership for profile and team endpoints using the authenticated email identity. */
@Component("employeeAccess")
public class EmployeeAccess {
    private final EmployeeRepository employees;
    public EmployeeAccess(EmployeeRepository employees) { this.employees = employees; }
    public boolean owns(Integer id, Authentication authentication) {
        return employees.findById(id).map(employee -> employee.getEmail().equalsIgnoreCase(authentication.getName())).orElse(false);
    }
}
