package com.netex.addressbook.auth;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByEmail(String email) {
        return jdbc.query("""
                SELECT id, email, password_hash, role FROM users WHERE lower(email) = lower(?)
                """, (rs, rowNum) -> new AppUser(
                rs.getLong("id"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("role")), email)
                .stream().findFirst();
    }

    public long create(String email, String passwordHash, String role) {
        Long id = jdbc.queryForObject("""
                INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?) RETURNING id
                """, Long.class, email, passwordHash, role);
        if (id == null) {
            throw new IllegalStateException("Insert did not return a user ID");
        }
        return id;
    }
}
