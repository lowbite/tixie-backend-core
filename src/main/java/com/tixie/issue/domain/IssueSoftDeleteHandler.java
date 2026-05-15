package com.tixie.issue.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class IssueSoftDeleteHandler {

    @Inject
    EntityManager em;

    @Transactional
    public void softDelete(UUID rootId) {
        em.createNativeQuery("""
                WITH RECURSIVE descendants AS (
                    SELECT id FROM issues WHERE id = :rootId
                    UNION ALL
                    SELECT i.id FROM issues i
                    INNER JOIN descendants d ON i.parent_id = d.id
                    WHERE i.deleted_at IS NULL
                )
                UPDATE issues
                SET deleted_at = now(), updated_at = now()
                WHERE id IN (SELECT id FROM descendants)
                """)
                .setParameter("rootId", rootId)
                .executeUpdate();
    }

    @Transactional
    public void softDeleteByProjectId(UUID projectId) {
        em.createNativeQuery("""
                UPDATE issues
                SET deleted_at = now(), updated_at = now()
                WHERE project_id = :projectId AND deleted_at IS NULL
                """)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @Transactional
    public void softDeleteByCompanyId(UUID companyId) {
        em.createNativeQuery("""
                UPDATE issues i
                SET deleted_at = now(), updated_at = now()
                FROM projects p
                WHERE i.project_id = p.id
                  AND p.company_id = :companyId
                  AND i.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                """)
                .setParameter("companyId", companyId)
                .executeUpdate();
    }
}
