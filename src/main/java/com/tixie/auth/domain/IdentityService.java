package com.tixie.auth.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class IdentityService {

    @Inject
    JsonWebToken jwt;

    public KeycloakIdentity currentIdentity() {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new NotAuthorizedException("Authenticated Keycloak subject is required");
        }

        String email = claim("email");
        if (email == null || email.isBlank()) {
            throw new NotAuthorizedException("Authenticated Keycloak email is required");
        }

        String name = claim("name");
        if (name == null || name.isBlank()) {
            name = claim("preferred_username");
        }

        return new KeycloakIdentity(subject, email, name);
    }

    private String claim(String name) {
        Object value = jwt.getClaim(name);
        return value == null ? null : value.toString();
    }
}
