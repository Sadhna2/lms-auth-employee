package com.nexturn.lms.service;

import com.nexturn.lms.dto.EmployeeRequest;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.*;
import com.nexturn.lms.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests profile CRUD invariants, manager selection and reporting-chain safety. */
class EmployeeServiceTest {
    EmployeeRepository employees;
    LoginRepository logins;
    EmployeeServiceImpl service;
    @BeforeEach void setup() {
        employees = mock(EmployeeRepository.class); logins = mock(LoginRepository.class);
        service = new EmployeeServiceImpl(employees, logins, mock(EmployeeEvents.class));
        when(employees.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    Employee profile(int id, String email) {
        Employee e = new Employee(); e.setEmpId(id); e.setEmail(email); e.setPhoneNumber("9876543210"); return e;
    }
    EmployeeRequest input(String email, Integer manager) {
        return new EmployeeRequest(" User ", email, "9876543210", "Developer", 1, manager, "Female", "Pune");
    }
    @Test void createsNormalizedProfileWithoutManualId() {
        Employee result = service.create(input("USER@example.com", null));
        assertNull(result.getEmpId()); assertEquals("user@example.com", result.getEmail()); assertEquals("User", result.getEmpName());
    }
    @Test void missingEmployeeIs404() { assertThrows(ResourceNotFoundException.class, () -> service.getById(999)); }
    @Test void rejectsDuplicateEmail() {
        when(employees.existsByEmail("user@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(input("user@example.com", null)));
    }
    @Test void rejectsDuplicatePhone() {
        when(employees.existsByPhoneNumber("9876543210")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(input("user@example.com", null)));
    }
    @Test void updateRequiresAnExistingEmployee() {
        assertThrows(ResourceNotFoundException.class, () -> service.update(999, input("user@example.com", null)));
        verify(employees, never()).save(any());
    }
    @Test void updateRetainsIdAndChangesProfile() {
        when(employees.findById(1)).thenReturn(Optional.of(profile(1,"user@example.com")));
        assertEquals(1, service.update(1,input("user@example.com",null)).getEmpId());
    }
    @Test void rejectsSelfManagement() {
        when(employees.findById(1)).thenReturn(Optional.of(profile(1,"manager@example.com")));
        when(logins.findById("manager@example.com")).thenReturn(Optional.of(new Login("manager@example.com", "hash", Role.MANAGER)));
        assertThrows(BusinessException.class, () -> service.update(1, input("manager@example.com", 1)));
    }
    @Test void rejectsIndirectReportingCycle() {
        Employee first = profile(1,"one@example.com"); Employee second = profile(2,"two@example.com"); second.setManagerId(1);
        when(employees.findById(1)).thenReturn(Optional.of(first)); when(employees.findById(2)).thenReturn(Optional.of(second));
        when(logins.findById("two@example.com")).thenReturn(Optional.of(new Login("two@example.com","hash",Role.MANAGER)));
        assertThrows(BusinessException.class, () -> service.update(1,input("one@example.com",2)));
    }
    @Test void rejectsNonManagerAssignment() {
        when(employees.findById(2)).thenReturn(Optional.of(profile(2,"user@example.com")));
        assertThrows(BusinessException.class, () -> service.create(input("new@example.com",2)));
    }
    @Test void listsManagersByAccountRole() {
        when(logins.findByRole(Role.MANAGER)).thenReturn(List.of(new Login("manager@example.com","hash",Role.MANAGER)));
        Employee manager = profile(2,"manager@example.com");
        when(employees.findByEmailIn(List.of("manager@example.com"))).thenReturn(List.of(manager));
        assertEquals(List.of(manager),service.getManagers());
    }
    @Test void returnsManagersDirectReports() {
        when(employees.findById(2)).thenReturn(Optional.of(profile(2,"manager@example.com")));
        when(logins.findById("manager@example.com")).thenReturn(Optional.of(new Login("manager@example.com","hash",Role.MANAGER)));
        Employee report = profile(3,"report@example.com"); when(employees.findByManagerId(2)).thenReturn(List.of(report));
        assertEquals(List.of(report),service.getTeam(2));
    }
    @Test void blocksDeletingManagerWithReports() {
        when(employees.findById(2)).thenReturn(Optional.of(profile(2,"manager@example.com")));
        when(employees.existsByManagerId(2)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.delete(2)); verify(employees,never()).delete(any());
    }
    @Test void deletesLinkedLoginAndEmployee() {
        Employee employee = profile(1,"user@example.com"); Login login = new Login("user@example.com","hash",Role.EMPLOYEE);
        when(employees.findById(1)).thenReturn(Optional.of(employee)); when(logins.findById(employee.getEmail())).thenReturn(Optional.of(login));
        service.delete(1); verify(logins).delete(login); verify(employees).delete(employee);
    }
    @Test void protectsLinkedLoginIdentityFromEmailChanges() {
        when(employees.findById(1)).thenReturn(Optional.of(profile(1,"old@example.com")));
        when(logins.existsById("old@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.update(1,input("new@example.com",null)));
    }

    @Test void currentUserProfileUsesNormalizedEmail() {
        Employee employee=profile(1,"user@example.com");
        when(employees.findByEmail("user@example.com")).thenReturn(Optional.of(employee));
        assertEquals(employee,service.getByEmail(" USER@EXAMPLE.COM "));
    }
    @Test void missingCurrentUserProfileIs404() {
        assertThrows(ResourceNotFoundException.class,()->service.getByEmail("missing@example.com"));
    }
}
