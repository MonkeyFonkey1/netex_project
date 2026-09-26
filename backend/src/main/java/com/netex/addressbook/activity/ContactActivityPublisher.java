package com.netex.addressbook.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContactActivityPublisher {

    private final EventOutboxRepository outbox;
    private final ObjectMapper json;

    public ContactActivityPublisher(EventOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    public void created(long contactId, long actorUserId) {
        queue(contactId, actorUserId, "CREATED");
    }

    public void updated(long contactId, long actorUserId) {
        queue(contactId, actorUserId, "UPDATED");
    }

    public void deleted(long contactId, long actorUserId) {
        queue(contactId, actorUserId, "DELETED");
    }

    private void queue(long contactId, long actorUserId, String action) {
        ContactActivityEvent event = new ContactActivityEvent(
                UUID.randomUUID(), contactId, actorUserId, action, Instant.now());
        try {
            outbox.enqueue("HTTP_CONTACT", event.eventId().toString(), json.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize contact event", exception);
        }
    }
}
