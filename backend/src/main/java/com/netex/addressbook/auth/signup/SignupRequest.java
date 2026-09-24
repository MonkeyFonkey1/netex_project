package com.netex.addressbook.auth.signup;

import com.netex.addressbook.auth.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    public SignupRequest {
        if (email != null) {
            email = EmailAddress.normalize(email);
        }
    }
}
