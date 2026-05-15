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
}
