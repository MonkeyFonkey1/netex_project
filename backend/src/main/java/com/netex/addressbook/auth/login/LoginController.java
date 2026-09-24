package com.netex.addressbook.auth.login;

import com.netex.addressbook.auth.AccountPrincipal;
import com.netex.addressbook.auth.EmailAddress;
import com.netex.addressbook.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/login")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public LoginController(AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository, CsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping
    public UserResponse login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            EmailAddress.normalize(body.email()), body.password()));
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        request.getSession(true);
        request.changeSessionId();
        csrfTokenRepository.saveToken(null, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return UserResponse.from((AccountPrincipal) authentication.getPrincipal());
    }
}
