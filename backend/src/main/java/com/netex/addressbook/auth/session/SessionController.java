package com.netex.addressbook.auth.session;

import com.netex.addressbook.auth.AppUser;
import com.netex.addressbook.auth.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUser user) {
        return UserResponse.from(user);
    }
}
