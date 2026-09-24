package com.netex.addressbook.auth.signup;

import java.nio.charset.StandardCharsets;
import com.netex.addressbook.auth.AppUser;
import com.netex.addressbook.auth.EmailAddress;
import com.netex.addressbook.auth.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SignupService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser signup(SignupRequest request) {
        String email = EmailAddress.normalize(request.email());
        if (email.isEmpty() || email.length() > 254 || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
        }

        String hash = passwordEncoder.encode(request.password());
        try {
            long id = users.create(email, hash, "USER");
            return new AppUser(id, email, hash, "USER");
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered", exception);
        }
    }
}
