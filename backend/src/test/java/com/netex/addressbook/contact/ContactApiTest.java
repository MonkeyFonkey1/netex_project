package com.netex.addressbook.contact;

import com.netex.addressbook.PostgresTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class ContactApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void emptyAddressBookIsPublicAndReturnsAnEmptyArray() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsContactsInIdOrderWithoutExposingInternalDatabaseFields() throws Exception {
        long authorId = createUser();
        long firstId = createContact(authorId, "Maria", "Strada 1");
        long secondId = createContact(authorId, "Andrei", "Strada 2");

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[0].name").value("Maria"))
                .andExpect(jsonPath("$[0].address").value("Strada 1"))
                .andExpect(jsonPath("$[0].canManage").value(false))
                .andExpect(jsonPath("$[0].createdByUserId").doesNotExist())
                .andExpect(jsonPath("$[0].picturePath").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(secondId));
    }

    @Test
    void searchesByCaseInsensitiveSubstringAndTrimsTheSearchTerm() throws Exception {
        long authorId = createUser();
        createContact(authorId, "Maria Popescu", "Strada 1");
        createContact(authorId, "Andrei Ionescu", "Strada 2");

        mockMvc.perform(get("/api/contacts").param("name", "  ARIA  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Maria Popescu"));

        mockMvc.perform(get("/api/contacts").param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void treatsSearchWildcardsAsOrdinaryText() throws Exception {
        long authorId = createUser();
        createContact(authorId, "Maria % Popescu", "Strada 1");
        createContact(authorId, "Maria Popescu", "Strada 2");

        mockMvc.perform(get("/api/contacts").param("name", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Maria % Popescu"));
    }

    @Test
    void rejectsAnExcessivelyLongSearchTerm() throws Exception {
        mockMvc.perform(get("/api/contacts").param("name", "a".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrievesAContactByIdAndReturnsNotFoundForAnUnknownId() throws Exception {
        long authorId = createUser();
        long contactId = createContact(authorId, "Maria", "Strada 1");

        mockMvc.perform(get("/api/contacts/{id}", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria"));

        mockMvc.perform(get("/api/contacts/{id}", -1))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportsFilteredUtf8CsvWithEscapedTextAndAPictureReference() throws Exception {
        long authorId = createUser();
        long contactId = createContact(authorId, "María, \"M.\"", "Strada 1,\nEtaj \"2\"");
        jdbc.update("UPDATE contacts SET picture_path = ? WHERE id = ?", "stored.png", contactId);
        createContact(authorId, "Andrei", "Altă adresă");
        long version = jdbc.queryForObject("SELECT updated_at FROM contacts WHERE id = ?",
                Timestamp.class, contactId).toInstant().toEpochMilli();

        var result = mockMvc.perform(get("/api/contacts/export").param("name", "  MARÍA  "))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"contacts.csv\"");
        assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .isEqualTo("\uFEFFname,address,picture_url\r\n"
                        + "\"María, \"\"M.\"\"\",\"Strada 1,\nEtaj \"\"2\"\"\","
                        + "\"/api/contacts/" + contactId + "/picture?v=" + version + "\"\r\n");
    }

    @Test
    void exportsOnlyTheHeaderWhenEmptyAndRejectsAnInvalidSearch() throws Exception {
        mockMvc.perform(get("/api/contacts/export"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(new String(result.getResponse().getContentAsByteArray(),
                        StandardCharsets.UTF_8)).isEqualTo("\uFEFFname,address,picture_url\r\n"));
        mockMvc.perform(get("/api/contacts/export").param("name", "a".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    private long createUser() {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password_hash)
                VALUES ('contact-api-test@example.com', 'test-only-hash') RETURNING id
                """, Long.class);
    }

    private long createContact(long authorId, String name, String address) {
        return jdbc.queryForObject("""
                INSERT INTO contacts (name, address, created_by_user_id)
                VALUES (?, ?, ?) RETURNING id
                """, Long.class, name, address, authorId);
    }
}
