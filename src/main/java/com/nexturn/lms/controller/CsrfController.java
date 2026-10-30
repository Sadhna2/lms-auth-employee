package com.nexturn.lms.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/** Supplies the CSRF token and cookie required for unsafe requests, including login. */
@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }
}
