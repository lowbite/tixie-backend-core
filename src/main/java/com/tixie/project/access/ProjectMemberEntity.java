package com.tixie.project.access;

import com.tixie.authz.ProjectRole;
import com.tixie.authz.SubjectType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_members")
public class ProjectMemberEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "project_id", nullable = false)
    public UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    public SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    public UUID subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ProjectRole role;

    @Column(name = "created_by_user_id", nullable = false)
    public UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "revoked_at")
    public Instant revokedAt;
}
