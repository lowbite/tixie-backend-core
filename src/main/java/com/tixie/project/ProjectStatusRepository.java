package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProjectStatusRepository implements PanacheRepositoryBase<ProjectStatusEntity, UUID> {

    public List<ProjectStatusEntity> findByProjectId(UUID projectId) {
        return list("projectId", projectId);
    }

    public Optional<ProjectStatusEntity> findByIdAndProjectId(UUID id, UUID projectId) {
        return find("id = ?1 and projectId = ?2", id, projectId).firstResultOptional();
    }
}
