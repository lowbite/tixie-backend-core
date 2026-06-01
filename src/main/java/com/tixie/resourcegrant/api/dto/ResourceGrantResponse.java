package com.tixie.resourcegrant.api.dto;

import com.tixie.authz.Permission;
import com.tixie.authz.ResourceType;
import com.tixie.authz.SubjectType;

import java.time.Instant;
import java.util.UUID;

public class ResourceGrantResponse {
    public UUID id;
    public UUID companyId;
    public ResourceType resourceType;
    public UUID resourceId;
    public SubjectType subjectType;
    public UUID subjectId;
    public Permission permission;
    public Instant expiresAt;
    public Instant createdAt;
}
