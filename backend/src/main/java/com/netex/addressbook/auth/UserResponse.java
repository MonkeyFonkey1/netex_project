package com.netex.addressbook.auth;

public record UserResponse(long id, String email, String role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.id(), user.email(), user.role());
    }

    public static UserResponse from(AccountPrincipal principal) {
        return new UserResponse(principal.id(), principal.email(), principal.role());
    }
}
