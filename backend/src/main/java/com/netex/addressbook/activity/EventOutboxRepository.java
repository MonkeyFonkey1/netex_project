package com.netex.addressbook.activity;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventOutboxRepository {

    private final JdbcTemplate jdbc;

    public EventOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(String destination, String eventKey, String payload) {
        jdbc.update("INSERT INTO event_outbox (destination, event_key, payload) VALUES (?, ?, ?)",
                destination, eventKey, payload);
    }

    public List<PendingEvent> due() {
        return jdbc.query("""
                SELECT id, destination, event_key, payload FROM event_outbox
                WHERE next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY id LIMIT 20
                """, (row, number) -> new PendingEvent(row.getLong("id"),
                row.getString("destination"), row.getString("event_key"), row.getString("payload")));
    }

    public void delivered(long id) {
        jdbc.update("DELETE FROM event_outbox WHERE id = ?", id);
    }

    public void retryLater(long id) {
        jdbc.update("""
                UPDATE event_outbox SET attempts = attempts + 1,
                    next_attempt_at = CURRENT_TIMESTAMP + INTERVAL '5 seconds'
                WHERE id = ?
                """, id);
    }

    public record PendingEvent(long id, String destination, String eventKey, String payload) {
    }
}
