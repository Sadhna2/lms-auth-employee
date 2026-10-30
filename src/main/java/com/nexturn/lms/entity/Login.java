package com.nexturn.lms.entity;

import com.nexturn.lms.enums.Role;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Login identity. Username is the employee email; only a BCrypt hash is stored. */
@Entity
@Table(name = "login")
public class Login {

    @Id
    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    public Login() {
    }

    public Login(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
