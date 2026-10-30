package com.nexturn.lms.service;

import com.nexturn.lms.dto.*;
import com.nexturn.lms.entity.Login;
import com.nexturn.lms.exception.*;
import com.nexturn.lms.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores only BCrypt hashes and returns safe identity projections without passwords. */
@Service
@Transactional(readOnly = true)
public class LoginServiceImpl implements LoginService {
    private final LoginRepository logins;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final String dummyHash;
    private final EmployeeEvents events;

    public LoginServiceImpl(LoginRepository logins, EmployeeRepository employees, PasswordEncoder encoder, EmployeeEvents events) {
        this.events = events;
        this.logins = logins;
        this.employees = employees;
        this.encoder = encoder;
        this.dummyHash = encoder.encode("dummy-password-for-unknown-account");
    }
    /** Uses a dummy BCrypt check for unknown users and one generic credential failure message. */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Login user = logins.findById(normalize(request.username())).orElse(null);
        boolean matches = encoder.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (user == null || !matches) throw new BadCredentialsException("Invalid username or password");
        events.record(employees.findByEmail(user.getUsername()).map(employee -> employee.getEmpId()).orElse(null),"LOGIN_SUCCEEDED");
        return new LoginResponse(user.getUsername(), user.getRole().name());
    }
    /** Creates a login for an existing profile; duplicate usernames never overwrite credentials. */
    @Override
    @Transactional
    public LoginResponse addUser(CreateUserRequest request) {
        String username = normalize(request.username());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Password must be at most 72 UTF-8 bytes for BCrypt");
        if (logins.existsById(username)) throw new ConflictException("Username already exists");
        if (!employees.existsByEmail(username)) throw new ResourceNotFoundException("Create the employee profile before its login user");
        Login user = logins.save(new Login(username, encoder.encode(request.password()), request.role()));
        events.record(employees.findByEmail(username).map(employee -> employee.getEmpId()).orElse(null),"ACCOUNT_CREATED");
        return new LoginResponse(user.getUsername(), user.getRole().name(),"Registration successful. Login account created.");
    }
    public static String normalize(String username) { return username.trim().toLowerCase(Locale.ROOT); }
}
