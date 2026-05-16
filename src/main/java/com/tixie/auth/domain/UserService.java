package com.tixie.auth.domain;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRepository;
import com.tixie.auth.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public Optional<UserEntity> findCurrentUser(KeycloakIdentity identity) {
        return userRepository.findActiveByKeycloakSubject(identity.subject());
    }

    public boolean activeEmailExists(String email) {
        return userRepository.findActiveByEmail(email).isPresent();
    }

    @Transactional
    public UserEntity create(KeycloakIdentity identity, UUID companyId, UserRole role) {
        userRepository.findActiveByKeycloakSubject(identity.subject()).ifPresent(existing -> {
            throw new ClientErrorException("User is already onboarded", Response.Status.CONFLICT);
        });
        userRepository.findActiveByEmail(identity.email()).ifPresent(existing -> {
            throw new ClientErrorException("Email is already linked to another user", Response.Status.CONFLICT);
        });

        var now = Instant.now();
        var user = new UserEntity();
        user.companyId = companyId;
        user.keycloakSubject = identity.subject();
        user.email = identity.email();
        user.displayName = identity.displayName();
        user.role = role;
        user.createdAt = now;
        user.updatedAt = now;
        userRepository.persist(user);
        return user;
    }
}
