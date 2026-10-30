package com.nexturn.lms.controller;

import com.nexturn.lms.dto.EmployeeRequest;
import com.nexturn.lms.dto.ManagerResponse;
import com.nexturn.lms.entity.Employee;
import com.nexturn.lms.service.EmployeeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Employee CRUD, manager listing and manager-team endpoints for Member 1. */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public List<Employee> getAll() { return service.getAll(); }
    /** Returns the caller's own profile without granting a full-directory permission. */
    @GetMapping("/me")
    public Employee me(org.springframework.security.core.Authentication authentication) {
        return service.getByEmail(authentication.getName());
    }
    @GetMapping("/managers")
    public List<ManagerResponse> managers() {
        return service.getManagers().stream().map(e -> new ManagerResponse(e.getEmpId(), e.getEmpName(), e.getEmail(), e.getDesignation())).toList();
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN') or @employeeAccess.owns(#id, authentication)")
    public Employee get(@PathVariable Integer id) { return service.getById(id); }
    @PostMapping @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Employee> create(@Valid @RequestBody EmployeeRequest request) {
        Employee employee = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + employee.getEmpId())).body(employee);
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public Employee update(@PathVariable Integer id, @Valid @RequestBody EmployeeRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/team")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN') or (hasRole('MANAGER') and @employeeAccess.owns(#id, authentication))")
    public List<Employee> team(@PathVariable Integer id) { return service.getTeam(id); }
}
