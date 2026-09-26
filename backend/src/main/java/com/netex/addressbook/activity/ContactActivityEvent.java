package com.netex.addressbook.activity;

import java.time.Instant;
import java.util.UUID;

public record ContactActivityEvent(UUID eventId, long contactId, long actorUserId,
        String action, Instant occurredAt) {
}
