package com.tixie.issue;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IssueKeyCounterRepository implements PanacheRepositoryBase<IssueKeyCounterEntity, UUID> {

    public void ensureExists(UUID projectId) {
        getEntityManager()
                .createNativeQuery("""
                        INSERT INTO issue_key_counters (project_id, last_seq)
                        VALUES (:projectId, 0)
                        ON CONFLICT (project_id) DO NOTHING
                        """)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public Optional<IssueKeyCounterEntity> findByProjectIdForUpdate(UUID projectId) {
        List<IssueKeyCounterEntity> result = getEntityManager()
                .createNativeQuery(
                        "SELECT * FROM issue_key_counters WHERE project_id = :projectId FOR UPDATE",
                        IssueKeyCounterEntity.class)
                .setParameter("projectId", projectId)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
