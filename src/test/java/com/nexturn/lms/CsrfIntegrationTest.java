package com.nexturn.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.repository.LoginRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests actual CSRF cookies without the csrf() postprocessor replacing the filter repository. */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:csrf-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CsrfIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired LoginRepository logins;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @BeforeEach void setup() { logins.save(new Login("admin@example.com",encoder.encode("admin-test-password"),Role.ADMIN)); }
    @Test void csrfEndpointProvidesCookieAndToken() throws Exception {
        mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.token",not(emptyString()))).andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }
    @Test void realCookieAndCsrfHeaderAllowLogin() throws Exception {
        var first=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String token=mapper.readTree(first.getContentAsString()).get("token").asText();
        mvc.perform(post("/api/login").cookie(first.getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN",token)
                .contentType("application/json").content("{\"username\":\"admin@example.com\",\"password\":\"admin-test-password\"}"))
                .andExpect(status().isOk());
    }
}
