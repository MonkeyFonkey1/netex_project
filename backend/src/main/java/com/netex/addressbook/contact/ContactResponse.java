package com.netex.addressbook.contact;

import com.netex.addressbook.auth.AppUser;

// The public API exposes an action hint, not the author's database ID or photo path.
public record ContactResponse(long id, String name, String address, boolean canManage, String pictureUrl) {

    public static ContactResponse from(Contact contact, AppUser user) {
        boolean canManage = user != null && (user.isAdmin() || user.id() == contact.createdByUserId());
        return new ContactResponse(contact.id(), contact.name(), contact.address(), canManage, pictureUrlFor(contact));
    }

    static String pictureUrlFor(Contact contact) {
        return contact.picturePath() == null ? null
                : "/api/contacts/" + contact.id() + "/picture?v=" + contact.updatedAt().toEpochMilli();
    }
}
