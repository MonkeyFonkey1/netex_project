package com.netex.addressbook.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ContactRepository {

    private static final String SELECT_CONTACTS = """
            SELECT id, name, address, picture_path, created_by_user_id, created_at, updated_at
            FROM contacts
            """;

    private static final RowMapper<Contact> CONTACT_MAPPER = (rs, rowNumber) -> new Contact(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("address"),
            rs.getString("picture_path"),
            rs.getLong("created_by_user_id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbc;

    public ContactRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Contact> findAll(String name) {
        if (name.isEmpty()) {
            return jdbc.query(SELECT_CONTACTS + "ORDER BY id", CONTACT_MAPPER);
        }

        // strpos treats %, _ and other characters as text, not LIKE wildcards.
        return jdbc.query(SELECT_CONTACTS
                + "WHERE strpos(lower(name), lower(?)) > 0 ORDER BY id", CONTACT_MAPPER, name);
    }

    public Optional<Contact> findById(long id) {
        return jdbc.query(SELECT_CONTACTS + "WHERE id = ?", CONTACT_MAPPER, id)
                .stream()
                .findFirst();
    }
}
