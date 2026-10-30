package com.nexturn.lms.service;

import com.nexturn.lms.dto.EmployeeRequest;
import com.nexturn.lms.entity.Employee;
import java.util.List;

/** Defines employee CRUD plus manager listing and direct-report team lookup. */
public interface EmployeeService {
    List<Employee> getAll();
    Employee getById(Integer id);
    Employee getByEmail(String email);
    Employee create(EmployeeRequest request);
    Employee update(Integer id, EmployeeRequest request);
    void delete(Integer id);
    List<Employee> getManagers();
    List<Employee> getTeam(Integer managerId);
}
