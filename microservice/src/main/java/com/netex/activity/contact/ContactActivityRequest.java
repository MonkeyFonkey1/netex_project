package com.netex.activity.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record ContactActivityRequest(
        @NotNull UUID eventId,
        @Positive long contactId,
        @Positive long actorUserId,
        @NotBlank @Pattern(regexp = "CREATED|UPDATED|DELETED") String action,
        @NotNull Instant occurredAt) {
}
