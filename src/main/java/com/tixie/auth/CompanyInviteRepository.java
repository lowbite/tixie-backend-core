package com.tixie.auth;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CompanyInviteRepository implements PanacheRepositoryBase<CompanyInviteEntity, UUID> {

    public Optional<CompanyInviteEntity> findPendingByTokenHash(String tokenHash, Instant now) {
        return find("""
                tokenHash = ?1
                and acceptedAt is null
                and revokedAt is null
                and expiresAt > ?2
                """, tokenHash, now).firstResultOptional();
    }
}
