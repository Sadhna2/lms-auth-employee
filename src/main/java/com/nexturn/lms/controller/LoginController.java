package com.nexturn.lms.controller;

import com.nexturn.lms.dto.*;
import com.nexturn.lms.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Exposes credential verification and HR/ADMIN login-user provisioning. */
@RestController
@RequestMapping("/api/login")
public class LoginController {
    private final LoginService service;
    public LoginController(LoginService service) { this.service = service; }
    @PostMapping public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    /** Provisioning is protected; callers cannot publicly register themselves as administrators. */
    @PostMapping("/user") @PreAuthorize("hasRole('ADMIN') or (hasRole('HR') and (#request.role().name() == 'EMPLOYEE' or #request.role().name() == 'MANAGER'))")
    public ResponseEntity<LoginResponse> addUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addUser(request));
    }
}
