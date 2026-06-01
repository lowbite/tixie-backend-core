package com.tixie.auth;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

    public Optional<UserEntity> findActiveByKeycloakSubject(String keycloakSubject) {
        return find("keycloakSubject = ?1 and disabledAt is null", keycloakSubject).firstResultOptional();
    }

    public Optional<UserEntity> findActiveByEmail(String email) {
        return find("lower(email) = lower(?1) and disabledAt is null", email).firstResultOptional();
    }

    public Optional<UserEntity> findActiveByIdAndCompanyId(UUID id, UUID companyId) {
        return find("id = ?1 and companyId = ?2 and disabledAt is null", id, companyId).firstResultOptional();
    }
}
