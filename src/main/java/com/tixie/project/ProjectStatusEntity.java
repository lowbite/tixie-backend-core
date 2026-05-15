package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "project_statuses")
public class ProjectStatusEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "project_id", nullable = false)
    public UUID projectId;

    @Column(nullable = false)
    public String name;

    @Column(name = "display_order", nullable = false)
    public int displayOrder;

    @Column(name = "is_default", nullable = false)
    public boolean isDefault;
}
