package com.nexturn.lms.repository;

import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persists login users and identifies manager accounts without loading unrelated modules. */
public interface LoginRepository extends JpaRepository<Login, String> {
    List<Login> findByRole(Role role);
}
