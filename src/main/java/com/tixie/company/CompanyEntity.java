package com.tixie.company;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class CompanyEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
