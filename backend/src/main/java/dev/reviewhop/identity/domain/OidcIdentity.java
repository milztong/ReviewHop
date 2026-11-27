package dev.reviewhop.identity.domain;

import java.net.URI;
import java.util.Objects;

public record OidcIdentity(URI issuer, String subject) {

    public OidcIdentity {
        Objects.requireNonNull(issuer, "issuer must not be null");
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }
}
