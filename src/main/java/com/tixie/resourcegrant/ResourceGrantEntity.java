package com.tixie.resourcegrant;

import com.tixie.authz.Permission;
import com.tixie.authz.ResourceType;
import com.tixie.authz.SubjectType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resource_grants")
public class ResourceGrantEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    public ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    public UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    public SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    public UUID subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Permission permission;

    @Column(name = "created_by_user_id", nullable = false)
    public UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(name = "revoked_at")
    public Instant revokedAt;
}
