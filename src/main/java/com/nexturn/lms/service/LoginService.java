package com.nexturn.lms.service;

import com.nexturn.lms.dto.*;

/** Defines BCrypt login verification and authorized account provisioning. */
public interface LoginService {
    LoginResponse login(LoginRequest request);
    LoginResponse addUser(CreateUserRequest request);
}
