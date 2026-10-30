package com.nexturn.lms.config;

import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.BusinessException;
import com.nexturn.lms.repository.LoginRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Checks first-administrator provisioning without shipping a default or plaintext password. */
class BootstrapAdminTest {
    @Test void provisionsOnlyWhenLoginTableIsEmpty() {
        var logins=mock(LoginRepository.class); var encoder=new BCryptPasswordEncoder(4);
        var bootstrap=new BootstrapAdmin(logins,encoder,"ADMIN@example.com","strong-password");
        bootstrap.run(null);
        var capture=org.mockito.ArgumentCaptor.forClass(Login.class); verify(logins).save(capture.capture());
        assertEquals("admin@example.com",capture.getValue().getUsername());
        assertEquals(Role.ADMIN,capture.getValue().getRole());
        assertTrue(encoder.matches("strong-password",capture.getValue().getPassword()));
    }
    @Test void neverOverwritesExistingUsers() {
        var logins=mock(LoginRepository.class); when(logins.count()).thenReturn(1L);
        new BootstrapAdmin(logins,new BCryptPasswordEncoder(4),"admin@example.com","strong-password").run(null);
        verify(logins,never()).save(any());
    }
    @Test void missingVariablesCreateNoDefaultAccount() {
        var logins=mock(LoginRepository.class);
        new BootstrapAdmin(logins,new BCryptPasswordEncoder(4),"","").run(null); verifyNoInteractions(logins);
    }
    @Test void rejectsWeakBootstrapPassword() {
        var bootstrap=new BootstrapAdmin(mock(LoginRepository.class),new BCryptPasswordEncoder(4),"admin@example.com","short");
        assertThrows(BusinessException.class,()->bootstrap.run(null));
    }
}
