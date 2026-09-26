package com.netex.addressbook.auth.signup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SignupEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SignupEventPublisher.class);
    public static final String TOPIC = "user-signups";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public SignupEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    public void publish(long userId, String email) {
        String message;
        try {
            message = json.writeValueAsString(new UserSignedUpEvent(userId, email, Instant.now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize signup event", exception);
        }

        // The account is already saved. Log delivery failures; do not falsely reject a created account.
        try {
            kafka.send(TOPIC, Long.toString(userId), message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Could not publish signup event for user {}", userId, error);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Could not publish signup event for user {}", userId, exception);
        }
    }
}
