package com.netex.activity.history;

import com.netex.activity.PostgresTestConfiguration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ActivityHistoryApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnsSavedKafkaAndHttpActivity() throws Exception {
        long userId = 900_000L + (UUID.randomUUID().getMostSignificantBits() & 0xfffff);
        String email = "history-" + UUID.randomUUID() + "@example.test";
        UUID eventId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbc.update("""
                INSERT INTO activity.signup_events (user_id, email, signed_up_at)
                VALUES (?, ?, ?)
                """, userId, email, now);
        jdbc.update("""
                INSERT INTO activity.contact_events
                    (event_id, contact_id, actor_user_id, action, occurred_at)
                VALUES (?, ?, ?, 'UPDATED', ?)
                """, eventId, 42L, userId, now);

        mockMvc.perform(get("/internal/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signups[*].email", hasItem(email)))
                .andExpect(jsonPath("$.contactChanges[*].eventId", hasItem(eventId.toString())));
    }
}
