package com.netex.addressbook.auth;

public record AppUser(long id, String email, String passwordHash, String role) {
}
