package com.tixie.issue;

import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issues")
public class IssueEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "issue_key", nullable = false, unique = true)
    public String issueKey;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public IssuePriority priority = IssuePriority.MEDIUM;

    @Column(name = "status_id", nullable = false)
    public UUID statusId;

    @Column(name = "project_id", nullable = false)
    public UUID projectId;

    @Column(name = "parent_id")
    public UUID parentId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "position", nullable = false)
    public long position;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
