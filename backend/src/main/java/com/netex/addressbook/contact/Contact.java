package com.netex.addressbook.contact;

import java.time.Instant;

// Represents one row from the contacts table inside the backend.
public record Contact(
        long id,
        String name,
        String address,
        String picturePath,
        long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
