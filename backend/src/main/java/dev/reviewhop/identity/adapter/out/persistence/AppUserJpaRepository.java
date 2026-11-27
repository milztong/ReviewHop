package dev.reviewhop.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByOidcIssuerAndOidcSubject(String issuer, String subject);

    @Modifying
    @Query(value = """
            INSERT INTO app_user (id, oidc_issuer, oidc_subject, timezone, created_at)
            VALUES (:id, :issuer, :subject, :timezone, :createdAt)
            ON CONFLICT (oidc_issuer, oidc_subject) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("issuer") String issuer,
            @Param("subject") String subject,
            @Param("timezone") String timezone,
            @Param("createdAt") Instant createdAt);
}
