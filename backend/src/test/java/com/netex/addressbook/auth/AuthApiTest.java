package com.netex.addressbook.auth;

import com.netex.addressbook.PostgresTestConfiguration;
import com.netex.addressbook.activity.ActivityHistoryClient;
import com.netex.addressbook.activity.ActivityHistoryResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.kafka.admin.auto-create=false", "app.outbox.enabled=false"})
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class AuthApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean ActivityHistoryClient activityHistory;

    @Test
    void signupCreatesOnlyAUserAndNeverReturnsAHash() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"  " + email.toUpperCase() + "  \",\"password\":\"strong-password\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM event_outbox WHERE destination = 'KAFKA_SIGNUP' AND event_key = ?
                """, Integer.class, Long.toString(id))).isEqualTo(1);
        assertThat(hash).startsWith("$2").isNotEqualTo("strong-password");
        assertThat(passwordEncoder.matches("strong-password", hash)).isTrue();

        mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM event_outbox WHERE destination = 'KAFKA_SIGNUP' AND event_key = ?
                """, Integer.class, Long.toString(id))).isEqualTo(1);
    }

    @Test
    void invalidSignupIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"" + "é".repeat(50) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csrfTokenIsRequiredForSignupAndCanBeFetchedAnonymously() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        String body = "{\"email\":\"" + uniqueEmail() + "\",\"password\":\"strong-password\"}";

        mockMvc.perform(post("/api/auth/signup").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/signup").session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void loginPersistsASessionAndLogoutInvalidatesIt() throws Exception {
        String email = uniqueEmail();
        jdbc.update("INSERT INTO users (email, password_hash) VALUES (?, ?)",
                email, passwordEncoder.encode("strong-password"));

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email).param("password", "wrong-password"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email.toUpperCase())
                        .param("password", "strong-password"))
                .andExpect(status().isNoContent())
                .andReturn();
        MockHttpSession signedIn = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(signedIn).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(signedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(signedIn))
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

        when(activityHistory.recent()).thenReturn(new ActivityHistoryResponse(List.of(), List.of()));

        mockMvc.perform(get("/api/admin/activities")).andExpect(status().isUnauthorized());
        MockHttpSession regularSession = login(regular);
        mockMvc.perform(get("/api/admin/activities").session(regularSession))
                .andExpect(status().isForbidden());
        verifyNoInteractions(activityHistory);
        MockHttpSession adminSession = login(admin);
        mockMvc.perform(get("/api/admin/activities").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signups").isArray())
                .andExpect(jsonPath("$.contactChanges").isArray());
        verify(activityHistory).recent();
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email).param("password", "strong-password"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

}
