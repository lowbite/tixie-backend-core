package com.tixie.group;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class GroupEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;

    @Column(nullable = false)
    public String name;

    @Column(name = "created_by_user_id", nullable = false)
    public UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
