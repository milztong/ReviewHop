package dev.reviewhop.identity.application.port.out;

import dev.reviewhop.identity.domain.OidcIdentity;

public interface CurrentOidcIdentity {

    OidcIdentity requireCurrent();
}
