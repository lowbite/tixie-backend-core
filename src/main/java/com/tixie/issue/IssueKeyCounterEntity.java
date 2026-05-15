package com.tixie.issue;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "issue_key_counters")
public class IssueKeyCounterEntity extends PanacheEntityBase {

    @Id
    @Column(name = "project_id")
    public UUID projectId;

    @Column(name = "last_seq", nullable = false)
    public long lastSeq;
}
