package com.nexturn.lms.service;

import com.nexturn.lms.dto.EmployeeRequest;
import com.nexturn.lms.entity.Employee;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.*;
import com.nexturn.lms.repository.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains profiles independently of leave, approval, notification and audit modules. */
@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employees;
    private final LoginRepository logins;

    private final EmployeeEvents events;

    public EmployeeServiceImpl(EmployeeRepository employees, LoginRepository logins, EmployeeEvents events) {
        this.events = events;
        this.employees = employees;
        this.logins = logins;
    }
    @Override public List<Employee> getAll() { return employees.findAll(); }
    @Override public Employee getById(Integer id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
    @Override public Employee getByEmail(String email) {
        return employees.findByEmail(LoginServiceImpl.normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to this login"));
    }
    @Override @Transactional
    public Employee create(EmployeeRequest request) { return apply(new Employee(), request); }
    @Override @Transactional
    public Employee update(Integer id, EmployeeRequest request) { return apply(getById(id), request); }

    /** Blocks deleting managers with reports and atomically removes the employee's login. */
    @Override @Transactional
    public void delete(Integer id) {
        Employee employee = getById(id);
        if (employees.existsByManagerId(id)) throw new ConflictException("Reassign the manager's team before deleting this employee");
        logins.findById(employee.getEmail()).ifPresent(logins::delete);
        employees.delete(employee);
        events.record(id, "DELETED");
    }
    @Override public List<Employee> getManagers() {
        List<String> emails = logins.findByRole(Role.MANAGER).stream().map(user -> user.getUsername()).toList();
        return emails.isEmpty() ? List.of() : employees.findByEmailIn(emails);
    }
    @Override public List<Employee> getTeam(Integer managerId) {
        requireManager(managerId);
        return employees.findByManagerId(managerId);
    }

    /** Validates unique contacts and an acyclic reporting chain before persisting a profile. */
    private Employee apply(Employee employee, EmployeeRequest request) {
        boolean creating = employee.getEmpId() == null;
        String email = LoginServiceImpl.normalize(request.email());
        if (!Objects.equals(employee.getEmail(), email) && employees.existsByEmail(email))
            throw new ConflictException("Email already exists");
        if (!Objects.equals(employee.getPhoneNumber(), request.phoneNumber()) && employees.existsByPhoneNumber(request.phoneNumber()))
            throw new ConflictException("Phone number already exists");
        // Login usernames are email identities. Keep existing accounts stable during profile edits.
        if (employee.getEmpId() != null && !Objects.equals(employee.getEmail(), email) && logins.existsById(employee.getEmail()))
            throw new ConflictException("Email cannot change while a login account exists");
        if (request.managerId() != null) {
            Employee manager = requireManager(request.managerId());
            var visited = new HashSet<Integer>();
            while (manager != null) {
                if (Objects.equals(manager.getEmpId(), employee.getEmpId()) || !visited.add(manager.getEmpId()))
                    throw new BusinessException("An employee cannot manage themselves or create a reporting cycle");
                manager = manager.getManagerId() == null ? null : getById(manager.getManagerId());
            }
        }
        employee.setEmpName(request.empName().trim()); employee.setEmail(email);
        employee.setPhoneNumber(request.phoneNumber()); employee.setDesignation(request.designation().trim());
        employee.setDeptId(request.deptId()); employee.setManagerId(request.managerId());
        employee.setGender(request.gender().trim()); employee.setAddress(request.address().trim());
        Employee saved = employees.save(employee);
        events.record(saved.getEmpId(), creating ? "CREATED" : "UPDATED");
        return saved;
    }
    private Employee requireManager(Integer id) {
        Employee manager = getById(id);
        boolean managerRole = logins.findById(manager.getEmail()).map(user -> user.getRole() == Role.MANAGER).orElse(false);
        if (!managerRole) throw new BusinessException("Selected employee does not have the MANAGER role");
        return manager;
    }
}
