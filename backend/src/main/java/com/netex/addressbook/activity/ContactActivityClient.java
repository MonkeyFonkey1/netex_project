package com.netex.addressbook.activity;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ContactActivityClient {

    private static final Logger log = LoggerFactory.getLogger(ContactActivityClient.class);
    private final RestClient http;

    public ContactActivityClient(RestClient.Builder builder,
            @Value("${app.activity-service.url}") String serviceUrl) {
        this.http = builder.baseUrl(serviceUrl).build();
    }

    public void created(long contactId, long actorUserId) {
        send(contactId, actorUserId, "CREATED");
    }

    public void updated(long contactId, long actorUserId) {
        send(contactId, actorUserId, "UPDATED");
    }

    public void deleted(long contactId, long actorUserId) {
        send(contactId, actorUserId, "DELETED");
    }

    private void send(long contactId, long actorUserId, String action) {
        ContactActivityEvent event = new ContactActivityEvent(
                UUID.randomUUID(), contactId, actorUserId, action, Instant.now());
        try {
            http.post().uri("/internal/contact-events")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            // The contact change has already succeeded; an unavailable history service is logged.
            log.error("Could not send {} activity for contact {}", action, contactId, exception);
        }
    }
}
