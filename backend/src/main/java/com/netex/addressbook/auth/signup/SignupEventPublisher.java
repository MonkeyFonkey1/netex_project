package com.netex.addressbook.auth.signup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netex.addressbook.activity.EventOutboxRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SignupEventPublisher {

    public static final String TOPIC = "user-signups";

    private final EventOutboxRepository outbox;
    private final ObjectMapper json;

    public SignupEventPublisher(EventOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    public void publish(long userId, String email) {
        String message;
        try {
            message = json.writeValueAsString(new UserSignedUpEvent(userId, email, Instant.now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize signup event", exception);
        }

        outbox.enqueue("KAFKA_SIGNUP", Long.toString(userId), message);
    }
}
