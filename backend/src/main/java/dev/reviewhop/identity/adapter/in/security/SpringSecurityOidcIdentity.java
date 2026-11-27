package dev.reviewhop.identity.adapter.in.security;

import dev.reviewhop.identity.application.port.out.CurrentOidcIdentity;
import dev.reviewhop.identity.domain.OidcIdentity;
import java.net.URI;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
final class SpringSecurityOidcIdentity implements CurrentOidcIdentity {

    @Override
    public OidcIdentity requireCurrent() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("A valid OIDC identity is required");
        }

        var jwt = jwtAuthentication.getToken();
        if (jwt.getIssuer() == null) {
            throw new AuthenticationCredentialsNotFoundException("The token has no issuer");
        }
        return new OidcIdentity(URI.create(jwt.getIssuer().toString()), jwt.getSubject());
    }
}
