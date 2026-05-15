package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProjectStatusRepository implements PanacheRepositoryBase<ProjectStatusEntity, UUID> {

    public List<ProjectStatusEntity> findActiveByProjectId(UUID projectId) {
        return list("projectId = ?1 and deletedAt is null", projectId);
    }

    public List<ProjectStatusEntity> findActiveByProjectIds(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return list("projectId in ?1 and deletedAt is null", projectIds);
    }

    public List<ProjectStatusEntity> findActiveByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list("id in ?1 and deletedAt is null", ids);
    }

    public Optional<ProjectStatusEntity> findActiveByIdAndProjectId(UUID id, UUID projectId) {
        return find("id = ?1 and projectId = ?2 and deletedAt is null", id, projectId).firstResultOptional();
    }

    public boolean existsActiveByName(UUID projectId, String name) {
        return count("projectId = ?1 and lower(name) = lower(?2) and deletedAt is null", projectId, name) > 0;
    }

    public boolean existsActiveByNameExcludingId(UUID projectId, UUID excludedId, String name) {
        return count("projectId = ?1 and id <> ?2 and lower(name) = lower(?3) and deletedAt is null",
                projectId, excludedId, name) > 0;
    }

    public Optional<ProjectStatusEntity> findActiveDefaultByProjectId(UUID projectId) {
        return find("projectId = ?1 and isDefault = true and deletedAt is null", projectId).firstResultOptional();
    }

    public int softDeleteActiveByProjectId(UUID projectId) {
        return update("deletedAt = CURRENT_TIMESTAMP where projectId = ?1 and deletedAt is null", projectId);
    }

    public int softDeleteActiveByCompanyId(UUID companyId) {
        return getEntityManager().createNativeQuery("""
                UPDATE project_statuses ps
                SET deleted_at = now()
                FROM projects p
                WHERE ps.project_id = p.id
                  AND p.company_id = :companyId
                  AND ps.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                """)
                .setParameter("companyId", companyId)
                .executeUpdate();
    }
}
