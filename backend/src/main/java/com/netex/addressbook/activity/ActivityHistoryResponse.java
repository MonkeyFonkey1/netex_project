package com.netex.addressbook.activity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityHistoryResponse(
        List<Signup> signups,
        List<ContactChange> contactChanges
) {
    public record Signup(long userId, String email, Instant signedUpAt, Instant processedAt) {
    }

    public record ContactChange(UUID eventId, long contactId, long actorUserId,
            String action, Instant occurredAt, Instant processedAt) {
    }
}
