package com.nexturn.lms.config;

import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.BusinessException;
import com.nexturn.lms.repository.LoginRepository;
import com.nexturn.lms.service.LoginServiceImpl;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optionally provisions the first administrator from environment variables; there is no default password. */
@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final LoginRepository logins;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    public BootstrapAdmin(LoginRepository logins, PasswordEncoder encoder,
            @Value("${app.bootstrap.username:}") String username, @Value("${app.bootstrap.password:}") String password) {
        this.logins = logins; this.encoder = encoder; this.username = username; this.password = password;
    }
    /** Only an empty login table can receive the initial administrator. Remove variables after setup. */
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) return;
        if (username.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Bootstrap requires a username and a password of at least 12 characters and at most 72 UTF-8 bytes");
        if (logins.count() == 0)
            logins.save(new Login(LoginServiceImpl.normalize(username), encoder.encode(password), Role.ADMIN));
    }
}
