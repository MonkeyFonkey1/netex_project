package com.netex.addressbook;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Test
    void startupMigratesAnEmptyDatabaseAndDoesNotReapplyTheMigration() {
        assertThat(jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class))
                .contains("users", "contacts", "flyway_schema_history");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    @Transactional
    void aContactReferencesItsAuthorAndGetsGeneratedIdAndTimestamps() {
        Long authorId = createUser("author@example.com");
        Long contactId = jdbc.queryForObject("""
                INSERT INTO contacts (name, address, created_by_user_id)
                VALUES (?, ?, ?) RETURNING id
                """, Long.class, "Maria Popescu", "Strada Exemplu 10", authorId);

        assertThat(contactId).isPositive();
        assertThat(jdbc.queryForObject("""
                SELECT u.email FROM contacts c JOIN users u ON u.id = c.created_by_user_id
                WHERE c.id = ?
                """, String.class, contactId)).isEqualTo("author@example.com");
        assertThat(jdbc.queryForObject("""
                SELECT created_at IS NOT NULL AND updated_at = created_at AND picture_path IS NULL
                FROM contacts WHERE id = ?
                """, Boolean.class, contactId)).isTrue();
    }

    @Test
    @Transactional
    void duplicateEmailsAreRejectedRegardlessOfLetterCase() {
        createUser("ana@example.com");
        assertThatThrownBy(() -> createUser("ANA@example.com"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @Transactional
    void newAccountsDefaultToUserAndOtherRolesAreRejected() {
        Long userId = createUser("role@example.com");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId))
                .isEqualTo("USER");
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?)",
                "invalid-role@example.com", "test-only-hash", "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void aContactCannotReferenceAMissingAuthor() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO contacts (name, address, created_by_user_id)
                VALUES ('Maria', 'Strada Exemplu 10', -1)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void deletingAnAuthorDoesNotSilentlyDeleteTheirContacts() {
        Long authorId = createUser("owner@example.com");
        jdbc.update("INSERT INTO contacts (name, address, created_by_user_id) VALUES (?, ?, ?)",
                "Maria", "Strada Exemplu 10", authorId);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id = ?", authorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void blankContactNamesAreRejected() {
        Long authorId = createUser("validation@example.com");
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO contacts (name, address, created_by_user_id) VALUES (?, ?, ?)",
                "   ", "Strada Exemplu 10", authorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long createUser(String email) {
        // Test-only placeholder: real hashes will be produced by Spring Security.
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash) VALUES (?, ?) RETURNING id",
                Long.class, email, "test-only-hash");
    }
}
