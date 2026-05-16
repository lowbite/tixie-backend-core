package com.tixie.auth.domain;

public record KeycloakIdentity(String subject, String email, String displayName) {
}
