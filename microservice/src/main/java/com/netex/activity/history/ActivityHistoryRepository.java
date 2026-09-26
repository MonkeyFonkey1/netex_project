package com.netex.activity.history;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityHistoryRepository {

    private final JdbcTemplate jdbc;

    public ActivityHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ActivityHistoryResponse recent() {
        List<ActivityHistoryResponse.Signup> signups = jdbc.query("""
                SELECT user_id, email, signed_up_at, processed_at
                FROM activity.signup_events
                ORDER BY processed_at DESC, user_id DESC
                LIMIT 100
                """, (row, number) -> new ActivityHistoryResponse.Signup(
                row.getLong("user_id"), row.getString("email"),
                row.getObject("signed_up_at", OffsetDateTime.class).toInstant(),
                row.getObject("processed_at", OffsetDateTime.class).toInstant()));

        List<ActivityHistoryResponse.ContactChange> contactChanges = jdbc.query("""
                SELECT event_id, contact_id, actor_user_id, action, occurred_at, processed_at
                FROM activity.contact_events
                ORDER BY processed_at DESC, event_id DESC
                LIMIT 100
                """, (row, number) -> new ActivityHistoryResponse.ContactChange(
                row.getObject("event_id", UUID.class), row.getLong("contact_id"),
                row.getLong("actor_user_id"), row.getString("action"),
                row.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                row.getObject("processed_at", OffsetDateTime.class).toInstant()));

        return new ActivityHistoryResponse(signups, contactChanges);
    }
}
