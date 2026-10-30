package com.nexturn.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real controllers, security filters, global advice and JPA against isolated H2. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BackendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired LoginRepository logins;
    @Autowired EmployeeRepository employees;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired EmployeeEventRepository outbox;
    static final String BODY = """
        {"empName":"Test User","email":"person@example.com","phoneNumber":"9876543210",
         "designation":"Developer","deptId":1,"managerId":null,"gender":"Female","address":"Pune"}
        """;
    @BeforeEach void setup() { logins.save(new Login("admin@example.com",encoder.encode("admin-test-password"),Role.ADMIN)); }
    @Test void loginVerifiesHashAndDoesNotExposePassword() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).contentType("application/json")
                .content("{\"username\":\"admin@example.com\",\"password\":\"admin-test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void badCredentialsUseGlobal401() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).contentType("application/json")
                .content("{\"username\":\"admin@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
    @Test void anonymousRequestsAreRejectedWithJson() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
    @Test void invalidBasicCredentialsUseGlobalHandler() throws Exception {
        mvc.perform(get("/api/employees").with(httpBasic("admin@example.com","wrong")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
    @Test void employeeCannotCreateUsers() throws Exception {
        mvc.perform(post("/api/login/user").with(user("employee@example.com").roles("EMPLOYEE")).with(csrf())
                .contentType("application/json").content("{\"username\":\"new@example.com\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void invalidProfileUsesFieldErrors() throws Exception {
        mvc.perform(post("/api/employees").with(user("admin@example.com").roles("ADMIN")).with(csrf())
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists());
    }
    @Test void malformedRoleReturns400() throws Exception {
        mvc.perform(post("/api/login/user").with(user("admin@example.com").roles("ADMIN")).with(csrf())
                .contentType("application/json").content("{\"username\":\"new@example.com\",\"password\":\"password123\",\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void missingEmployeeReturns404() throws Exception {
        mvc.perform(get("/api/employees/9999").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
    @Test void basicAuthenticatedCrudAndAccountLifecycle() throws Exception {
        var response = mvc.perform(post("/api/employees").with(httpBasic("admin@example.com","admin-test-password"))
                .with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isCreated()).andReturn();
        int id = mapper.readTree(response.getResponse().getContentAsString()).get("empId").asInt();
        assertTrue(id > 0);
        assertEquals(1,outbox.count());
        assertEquals("CREATED",outbox.findAll().get(0).getEventType());
        mvc.perform(post("/api/login/user").with(user("admin@example.com").roles("ADMIN")).with(csrf())
                .contentType("application/json").content("{\"username\":\"person@example.com\",\"password\":\"password123\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        assertTrue(encoder.matches("password123", logins.findById("person@example.com").orElseThrow().getPassword()));
        mvc.perform(get("/api/employees/"+id).with(httpBasic("person@example.com","password123"))).andExpect(status().isOk());
        mvc.perform(put("/api/employees/"+id).with(user("admin@example.com").roles("ADMIN")).with(csrf())
                .contentType("application/json").content(BODY.replace("Test User","Updated User")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.empName").value("Updated User"));
        mvc.perform(delete("/api/employees/"+id).with(user("admin@example.com").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertFalse(logins.existsById("person@example.com")); assertFalse(employees.existsById(id));
        assertEquals(4,outbox.count());
        assertTrue(outbox.findAll().stream().anyMatch(e -> e.getEventType().equals("DELETED")));
        assertTrue(outbox.findAll().stream().anyMatch(e -> e.getEventType().equals("ACCOUNT_CREATED")));
    }
    @Test void duplicateEmployeeReturns409() throws Exception {
        mvc.perform(post("/api/employees").with(user("admin@example.com").roles("ADMIN")).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isCreated());
        mvc.perform(post("/api/employees").with(user("admin@example.com").roles("ADMIN")).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void browserUnsafeRequestsRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test void employeeCannotReadAnotherProfileOrAllProfiles() throws Exception {
        mvc.perform(get("/api/employees/9999").with(user("person@example.com").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").with(user("person@example.com").roles("EMPLOYEE"))).andExpect(status().isForbidden());
    }
    @Test void managerCanOnlyReadTheirOwnTeam() throws Exception {
        Employee manager = new Employee(); manager.setEmpName("Manager"); manager.setEmail("manager@example.com");
        manager.setPhoneNumber("9123456780"); manager.setDesignation("Lead"); manager.setGender("Female"); manager.setAddress("Pune");
        manager = employees.saveAndFlush(manager); int id = manager.getEmpId();
        logins.save(new Login(manager.getEmail(),encoder.encode("manager-password"),Role.MANAGER));
        mvc.perform(get("/api/employees/"+id+"/team").with(httpBasic("manager@example.com","manager-password")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/employees/9999/team").with(user("manager@example.com").roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees/managers").with(user("person@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].empId").value(id));
    }

    @Test void hrCannotProvisionAdministrator() throws Exception {
        mvc.perform(post("/api/login/user").with(user("hr@example.com").roles("HR")).with(csrf())
                .contentType("application/json").content("{\"username\":\"new@example.com\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

}
