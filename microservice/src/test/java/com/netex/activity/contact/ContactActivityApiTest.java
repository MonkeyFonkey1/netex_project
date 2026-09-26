package com.netex.activity.contact;

import com.netex.activity.PostgresTestConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ContactActivityApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void acceptsAContactEventAndIgnoresRepeatedDelivery() throws Exception {
        UUID eventId = UUID.randomUUID();
        String body = """
                {"eventId":"%s","contactId":42,"actorUserId":7,
                 "action":"CREATED","occurredAt":"2026-09-26T10:00:00Z"}
                """.formatted(eventId);

        mockMvc.perform(post("/internal/contact-events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/internal/contact-events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM activity.contact_events WHERE event_id = ?", Integer.class, eventId);
        String action = jdbc.queryForObject(
                "SELECT action FROM activity.contact_events WHERE event_id = ?", String.class, eventId);
        assertThat(count).isEqualTo(1);
        assertThat(action).isEqualTo("CREATED");
    }

    @Test
    void rejectsInvalidContactEvents() throws Exception {
        UUID eventId = UUID.randomUUID();
        String body = """
                {"eventId":"%s","contactId":0,"actorUserId":7,
                 "action":"OTHER","occurredAt":"2026-09-26T10:00:00Z"}
                """.formatted(eventId);

        mockMvc.perform(post("/internal/contact-events")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM activity.contact_events WHERE event_id = ?", Integer.class, eventId))
                .isZero();
    }
}
