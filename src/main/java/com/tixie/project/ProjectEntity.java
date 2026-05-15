package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true)
    public String key;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
