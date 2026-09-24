package com.netex.addressbook.auth.session;

import com.netex.addressbook.auth.AccountPrincipal;
import com.netex.addressbook.auth.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return UserResponse.from(principal);
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
