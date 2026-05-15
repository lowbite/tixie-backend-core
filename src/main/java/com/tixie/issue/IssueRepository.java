package com.tixie.issue;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IssueRepository implements PanacheRepositoryBase<IssueEntity, UUID> {

    public Optional<IssueEntity> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    public List<IssueEntity> findActiveByProjectId(UUID projectId) {
        return list("projectId = ?1 and deletedAt is null", projectId);
    }
}
