package com.netex.activity.contact;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContactActivityRepository {

    private final JdbcTemplate jdbc;

    public ContactActivityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean saveIfNew(ContactActivityRequest event) {
        int inserted = jdbc.update("""
                INSERT INTO activity.contact_events
                    (event_id, contact_id, actor_user_id, action, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, event.eventId(), event.contactId(), event.actorUserId(), event.action(),
                OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
        return inserted == 1;
    }
}
