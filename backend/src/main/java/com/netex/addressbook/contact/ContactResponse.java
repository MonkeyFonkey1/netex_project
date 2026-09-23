package com.netex.addressbook.contact;

// Only fields intended for the public API are serialized to JSON.
public record ContactResponse(long id, String name, String address) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.id(), contact.name(), contact.address());
    }
}
