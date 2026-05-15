package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProjectRepository implements PanacheRepositoryBase<ProjectEntity, UUID> {

    public Optional<ProjectEntity> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    public List<ProjectEntity> findActiveByCompanyId(UUID companyId) {
        return list("companyId = ?1 and deletedAt is null", companyId);
    }

    public boolean existsByKey(String key) {
        return count("key = ?1", key) > 0;
    }
}
