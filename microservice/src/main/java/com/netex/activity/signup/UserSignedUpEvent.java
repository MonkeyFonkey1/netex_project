package com.netex.activity.signup;

import java.time.Instant;

public record UserSignedUpEvent(long userId, String email, Instant signedUpAt) {
}
