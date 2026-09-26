package com.netex.activity.signup;

import com.netex.activity.PostgresTestConfiguration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Import(PostgresTestConfiguration.class)
class SignupEventConsumerTest {

    @Autowired SignupEventConsumer consumer;
    @Autowired JdbcTemplate jdbc;

    @Test
    void savesSignupAndIgnoresDuplicateDelivery() throws Exception {
        long userId = Math.abs(UUID.randomUUID().getMostSignificantBits() >> 1);
        String email = "kafka-" + UUID.randomUUID() + "@example.com";
        String message = """
                {"userId":%d,"email":"%s","signedUpAt":"2026-09-26T10:00:00Z"}
                """.formatted(userId, email);

        consumer.consume(message);
        consumer.consume(message);

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM activity.signup_events WHERE user_id = ?", Integer.class, userId);
        String storedEmail = jdbc.queryForObject(
                "SELECT email FROM activity.signup_events WHERE user_id = ?", String.class, userId);
        OffsetDateTime processedAt = jdbc.queryForObject(
                "SELECT processed_at FROM activity.signup_events WHERE user_id = ?", OffsetDateTime.class, userId);
        assertThat(count).isEqualTo(1);
        assertThat(storedEmail).isEqualTo(email);
        assertThat(processedAt).isNotNull();
    }
}
