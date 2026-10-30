package com.nexturn.lms.service;

import com.nexturn.lms.dto.*;
import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.*;
import com.nexturn.lms.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies real BCrypt matching, safe responses and account provisioning rules. */
class LoginServiceTest {
    LoginRepository logins;
    EmployeeRepository employees;
    BCryptPasswordEncoder encoder;
    LoginServiceImpl service;
    EmployeeEvents events;
    @BeforeEach void setup() {
        logins = mock(LoginRepository.class); employees = mock(EmployeeRepository.class);
        events=mock(EmployeeEvents.class);encoder = new BCryptPasswordEncoder(4); service = new LoginServiceImpl(logins, employees, encoder,events);
    }
    @Test void verifiesBcryptAndNormalizesUsername() {
        when(logins.findById("user@example.com")).thenReturn(Optional.of(new Login("user@example.com", encoder.encode("password123"), Role.EMPLOYEE)));
        assertEquals(new LoginResponse("user@example.com", "EMPLOYEE"), service.login(new LoginRequest(" USER@EXAMPLE.COM ", "password123")));
        verify(events).record(null,"LOGIN_SUCCEEDED");
    }
    @Test void rejectsWrongPassword() {
        when(logins.findById("user@example.com")).thenReturn(Optional.of(new Login("user@example.com", encoder.encode("password123"), Role.EMPLOYEE)));
        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("user@example.com", "wrong")));
    }
    @Test void rejectsUnknownUserWithSameMessage() {
        var error = assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("missing@example.com", "wrong")));
        assertEquals("Invalid username or password", error.getMessage());
        verifyNoInteractions(events);
    }
    @Test void rejectsMalformedStoredHashWithoutTryCatch() {
        when(logins.findById("user@example.com")).thenReturn(Optional.of(new Login("user@example.com", "plain-text", Role.EMPLOYEE)));
        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("user@example.com", "plain-text")));
    }
    @Test void storesBcryptInsteadOfPlainPassword() {
        when(employees.existsByEmail("user@example.com")).thenReturn(true);
        when(logins.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.addUser(new CreateUserRequest("USER@example.com", "password123", Role.MANAGER));
        var captor = org.mockito.ArgumentCaptor.forClass(Login.class);
        verify(logins).save(captor.capture());
        assertNotEquals("password123", captor.getValue().getPassword());
        assertTrue(encoder.matches("password123", captor.getValue().getPassword()));
        assertEquals("MANAGER", result.role());
        assertEquals("Registration successful. Login account created.",result.message());
        verify(events).record(null,"ACCOUNT_CREATED");
    }
    @Test void rejectsDuplicateUsername() {
        when(logins.existsById("user@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.addUser(new CreateUserRequest("user@example.com", "password123", Role.EMPLOYEE)));
        verify(logins, never()).save(any());
    }
    @Test void requiresEmployeeBeforeLoginUser() {
        assertThrows(ResourceNotFoundException.class, () -> service.addUser(new CreateUserRequest("user@example.com", "password123", Role.EMPLOYEE)));
    }
    @Test void rejectsMoreThan72Utf8Bytes() {
        assertThrows(BusinessException.class, () -> service.addUser(new CreateUserRequest("user@example.com", "Ã©".repeat(40), Role.EMPLOYEE)));
    }
}
