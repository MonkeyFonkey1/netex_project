package com.netex.addressbook.contact;

import com.netex.addressbook.PostgresTestConfiguration;
import com.netex.addressbook.activity.ContactActivityClient;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ContactMutationApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean ContactActivityClient activity;

    @Test
    void anonymousVisitorCannotCreateUpdateOrDelete() throws Exception {
        String body = "{\"name\":\"Maria\",\"address\":\"Strada 1\"}";
        mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/contacts/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/contacts/1"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(activity);
    }

    @Test
    void creatorComesFromSessionAndOnlyTheAuthorGetsManagementHint() throws Exception {
        TestAccount author = account();
        TestAccount other = account();
        String uniqueName = "Maria-" + UUID.randomUUID();
        String body = "{\"name\":\"  " + uniqueName + "  \",\"address\":\"  Strada 1  \",\"createdByUserId\":"
                + other.id() + "}";

        MvcResult created = mockMvc.perform(post("/api/contacts").session(author.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueName))
                .andExpect(jsonPath("$.address").value("Strada 1"))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.createdByUserId").doesNotExist())
                .andReturn();

        long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/contacts/" + id);
        assertThat(jdbc.queryForObject("SELECT created_by_user_id FROM contacts WHERE id = ?", Long.class, id))
                .isEqualTo(author.id());
        verify(activity).created(id, author.id());

        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canManage").value(false));
        mockMvc.perform(get("/api/contacts").param("name", uniqueName).session(author.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].canManage").value(true));
        mockMvc.perform(get("/api/contacts/{id}", id).session(other.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canManage").value(false));
    }

    @Test
    void invalidContactDataIsRejected() throws Exception {
        TestAccount author = account();
        mockMvc.perform(post("/api/contacts").session(author.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"address\":\"Strada 1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/contacts").session(author.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maria\",\"address\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contacts WHERE created_by_user_id = ?",
                Integer.class, author.id())).isZero();
        verifyNoInteractions(activity);
    }

    @Test
    void regularUsersCanEditOnlyTheirOwnContactAndThePhotoPathIsPreserved() throws Exception {
        TestAccount author = account();
        TestAccount other = account();
        long id = contact(author.id(), "Maria", "Strada 1", "saved-photo.jpg");
        String body = "{\"name\":\"  Maria Noua  \",\"address\":\"  Strada 2  \"}";

        mockMvc.perform(put("/api/contacts/{id}", id).session(other.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT name FROM contacts WHERE id = ?", String.class, id))
                .isEqualTo("Maria");

        mockMvc.perform(put("/api/contacts/{id}", id).session(author.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Noua"))
                .andExpect(jsonPath("$.address").value("Strada 2"))
                .andExpect(jsonPath("$.canManage").value(true));
        assertThat(jdbc.queryForObject("SELECT picture_path FROM contacts WHERE id = ?", String.class, id))
                .isEqualTo("saved-photo.jpg");
        verify(activity).updated(id, author.id());
        mockMvc.perform(put("/api/contacts/{id}", -1).session(author.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void regularUsersCanDeleteOnlyTheirOwnContact() throws Exception {
        TestAccount author = account();
        TestAccount other = account();
        long id = contact(author.id(), "Maria", "Strada 1", null);

        mockMvc.perform(delete("/api/contacts/{id}", id).session(other.session()))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contacts WHERE id = ?", Integer.class, id))
                .isEqualTo(1);

        mockMvc.perform(delete("/api/contacts/{id}", id).session(author.session()))
                .andExpect(status().isNoContent());
        verify(activity).deleted(id, author.id());
        mockMvc.perform(delete("/api/contacts/{id}", id).session(author.session()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateAndManageContactsCreatedByOtherUsers() throws Exception {
        TestAccount author = account();
        TestAccount admin = account("ADMIN");
        long authoredId = contact(author.id(), "Maria", "Strada 1", null);
        String body = "{\"name\":\"Admin Contact\",\"address\":\"Strada 2\"}";

        MvcResult created = mockMvc.perform(post("/api/contacts").session(admin.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canManage").value(true))
                .andReturn();
        long createdId = ((Number) com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT created_by_user_id FROM contacts WHERE id = ?",
                Long.class, createdId)).isEqualTo(admin.id());
        verify(activity).created(createdId, admin.id());

        mockMvc.perform(get("/api/contacts/{id}", authoredId).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canManage").value(true));
        mockMvc.perform(put("/api/contacts/{id}", authoredId).session(admin.session())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Contact"));
        verify(activity).updated(authoredId, admin.id());

        mockMvc.perform(delete("/api/contacts/{id}", authoredId).session(admin.session()))
                .andExpect(status().isNoContent());
        verify(activity).deleted(authoredId, admin.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contacts WHERE id = ?",
                Integer.class, authoredId)).isZero();
    }

    private TestAccount account() throws Exception {
        return account("USER");
    }

    private TestAccount account(String role) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        long id = jdbc.queryForObject("INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?) RETURNING id",
                Long.class, email, passwordEncoder.encode("strong-password"), role);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .param("email", email).param("password", "strong-password"))
                .andExpect(status().isNoContent()).andReturn();
        return new TestAccount(id, (MockHttpSession) result.getRequest().getSession(false));
    }

    private long contact(long authorId, String name, String address, String picturePath) {
        return jdbc.queryForObject("""
                INSERT INTO contacts (name, address, picture_path, created_by_user_id)
                VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, name, address, picturePath, authorId);
    }

    private record TestAccount(long id, MockHttpSession session) {
    }
}
