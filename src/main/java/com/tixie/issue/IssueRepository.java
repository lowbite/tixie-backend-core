package com.tixie.issue;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
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
        return list("projectId = ?1 and deletedAt is null order by position asc, createdAt asc", projectId);
    }

    public List<IssueEntity> findActiveByProjectId(UUID projectId, int page, int size) {
        return find("projectId = ?1 and deletedAt is null order by position asc, createdAt asc", projectId)
                .page(Page.of(page, size))
                .list();
    }

    public List<IssueEntity> findActiveByProjectIdAndStatusId(UUID projectId, UUID statusId) {
        return list("projectId = ?1 and statusId = ?2 and deletedAt is null order by position asc, createdAt asc",
                projectId, statusId);
    }

    public long nextPosition(UUID projectId, UUID statusId) {
        Long max = (Long) getEntityManager()
                .createQuery("""
                        select max(i.position)
                        from IssueEntity i
                        where i.projectId = :projectId and i.statusId = :statusId and i.deletedAt is null
                        """)
                .setParameter("projectId", projectId)
                .setParameter("statusId", statusId)
                .getSingleResult();
        return (max == null ? 0L : max) + 1L;
    }

    public void lockProjectStatusLane(UUID projectId, UUID statusId) {
        getEntityManager()
                .createNativeQuery("""
                        SELECT pg_advisory_xact_lock(
                            hashtext(CAST(:projectId AS text)),
                            hashtext(CAST(:statusId AS text))
                        )
                        """)
                .setParameter("projectId", projectId)
                .setParameter("statusId", statusId)
                .getSingleResult();
    }

    public long countActiveByProjectIdAndStatusId(UUID projectId, UUID statusId) {
        return count("projectId = ?1 and statusId = ?2 and deletedAt is null", projectId, statusId);
    }

    public int moveActiveIssuesToStatus(UUID projectId, UUID fromStatusId, UUID toStatusId) {
        return getEntityManager().createQuery("""
                        update IssueEntity i
                        set i.statusId = :toStatusId, i.updatedAt = CURRENT_TIMESTAMP
                        where i.projectId = :projectId and i.statusId = :fromStatusId and i.deletedAt is null
                        """)
                .setParameter("projectId", projectId)
                .setParameter("fromStatusId", fromStatusId)
                .setParameter("toStatusId", toStatusId)
                .executeUpdate();
    }
}
