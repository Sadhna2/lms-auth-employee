package com.nexturn.lms.repository;

import com.nexturn.lms.entity.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Queries employee profiles, unique contacts and direct manager reports. */
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByManagerId(Integer managerId);
    List<Employee> findByManagerId(Integer managerId);
    List<Employee> findByEmailIn(List<String> emails);
}
