package com.netex.activity.signup;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SignupEventRepository {

    private final JdbcTemplate jdbc;

    public SignupEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean saveIfNew(UserSignedUpEvent event) {
        int inserted = jdbc.update("""
                INSERT INTO activity.signup_events (user_id, email, signed_up_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO NOTHING
                """, event.userId(), event.email(), OffsetDateTime.ofInstant(event.signedUpAt(), ZoneOffset.UTC));
        return inserted == 1;
    }
}
