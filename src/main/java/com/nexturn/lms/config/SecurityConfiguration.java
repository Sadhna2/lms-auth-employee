package com.nexturn.lms.config;

import com.nexturn.lms.repository.LoginRepository;
import com.nexturn.lms.service.LoginServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Stateless HTTP Basic access. Use HTTPS and supply credentials on each protected request. */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
    @Bean public UserDetailsService userDetailsService(LoginRepository logins) {
        return username -> logins.findById(LoginServiceImpl.normalize(username))
                .map(user -> User.withUsername(user.getUsername()).password(user.getPassword()).roles(user.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
    }
    /** Security-filter failures use the same global advice as MVC exceptions. */
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        return http.csrf(csrf -> csrf
                        .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/csrf").permitAll().anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, ex) -> resolver.resolveException(request, response, null, ex)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> resolver.resolveException(request, response, null, ex))
                        .accessDeniedHandler((request, response, ex) -> resolver.resolveException(request, response, null, ex)))
                .build();
    }
}
