package com.tixie.resourcegrant.api.dto;

import com.tixie.authz.Permission;
import com.tixie.authz.SubjectType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class CreateResourceGrantRequest {
    @NotNull
    public SubjectType subjectType;

    @NotNull
    public UUID subjectId;

    @NotNull
    public Permission permission;

    public Instant expiresAt;
}
