package com.netex.addressbook.auth;

import com.netex.addressbook.PostgresTestConfiguration;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class AuthApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void signupNeedsCsrfCreatesOnlyAUserAndNeverReturnsAHash() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"  " + email.toUpperCase() + "  \",\"password\":\"strong-password\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        CsrfSession csrf = csrfSession();
        mockMvc.perform(post("/api/auth/signup").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get("/api/auth/me").session(csrf.session()))
                .andExpect(status().isUnauthorized());

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo("strong-password");
        assertThat(passwordEncoder.matches("strong-password", hash)).isTrue();

        mockMvc.perform(post("/api/auth/signup").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSignupIsRejected() throws Exception {
        CsrfSession csrf = csrfSession();
        mockMvc.perform(post("/api/auth/signup").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/signup").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"" + "é".repeat(50) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginPersistsASessionRotatesCsrfAndLogoutInvalidatesIt() throws Exception {
        String email = uniqueEmail();
        jdbc.update("INSERT INTO users (email, password_hash) VALUES (?, ?)",
                email, passwordEncoder.encode("strong-password"));

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        CsrfSession csrf = csrfSession();
        String anonymousSessionId = csrf.session().getId();
        mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .param("email", email).param("password", "strong-password"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .param("email", email).param("password", "wrong-password"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .param("email", email.toUpperCase())
                        .param("password", "strong-password"))
                .andExpect(status().isNoContent())
                .andReturn();
        MockHttpSession signedIn = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(signedIn).isNotNull();
        assertThat(signedIn.getId()).isNotEqualTo(anonymousSessionId);

        mockMvc.perform(get("/api/auth/me").session(signedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(post("/api/auth/logout").session(signedIn)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isForbidden());

        CsrfSession fresh = csrfSession(signedIn);
        mockMvc.perform(post("/api/auth/logout").session(signedIn)
                        .header(fresh.headerName(), fresh.token()))
                .andExpect(status().isNoContent());
        assertThat(signedIn.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminAreaIsProtectedByRoleOnTheServer() throws Exception {
        String regular = uniqueEmail();
        String admin = uniqueEmail();
        jdbc.update("INSERT INTO users (email, password_hash, role) VALUES (?, ?, 'USER')",
                regular, passwordEncoder.encode("strong-password"));
        jdbc.update("INSERT INTO users (email, password_hash, role) VALUES (?, ?, 'ADMIN')",
                admin, passwordEncoder.encode("strong-password"));

        mockMvc.perform(get("/api/admin/activities")).andExpect(status().isUnauthorized());
        MockHttpSession regularSession = login(regular);
        mockMvc.perform(get("/api/admin/activities").session(regularSession))
                .andExpect(status().isForbidden());
        MockHttpSession adminSession = login(admin);
        mockMvc.perform(get("/api/admin/activities").session(adminSession))
                .andExpect(status().isNotFound());
    }

    private MockHttpSession login(String email) throws Exception {
        CsrfSession csrf = csrfSession();
        MvcResult result = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .param("email", email).param("password", "strong-password"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private CsrfSession csrfSession() throws Exception {
        return csrfSession(null);
    }

    private CsrfSession csrfSession(MockHttpSession current) throws Exception {
        var request = get("/api/auth/csrf");
        if (current != null) {
            request.session(current);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return new CsrfSession((MockHttpSession) session, "X-CSRF-TOKEN", token);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {
    }
}
