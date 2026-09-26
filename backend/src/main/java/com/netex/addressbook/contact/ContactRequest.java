package com.netex.addressbook.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 1000) String address
) {
}
