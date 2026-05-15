package com.tixie.issue;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IssueRepositoryTest {

    @Test
    void methods_delegateToPanacheAndEntityManager() {
        var repo = spy(new IssueRepository());
        @SuppressWarnings("unchecked")
        PanacheQuery<IssueEntity> query = mock(PanacheQuery.class);
        UUID issueId = UUID.randomUUID();
        doReturn(query).when(repo).find("id = ?1 and deletedAt is null", issueId);
        when(query.firstResultOptional()).thenReturn(Optional.of(new IssueEntity()));
        UUID projectId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        doReturn(2L).when(repo).count("projectId = ?1 and statusId = ?2 and deletedAt is null", projectId, statusId);

        EntityManager em = mock(EntityManager.class);
        Query jpaQuery = mock(Query.class);
        doReturn(em).when(repo).getEntityManager();
        when(em.createQuery(anyString())).thenReturn(jpaQuery);
        when(jpaQuery.setParameter(anyString(), any())).thenReturn(jpaQuery);
        when(jpaQuery.getSingleResult()).thenReturn(5L);
        when(jpaQuery.executeUpdate()).thenReturn(1);

        assertTrue(repo.findActiveById(issueId).isPresent());
        assertEquals(2L, repo.countActiveByProjectIdAndStatusId(projectId, statusId));
        assertEquals(6L, repo.nextPosition(projectId, statusId));
        assertEquals(1, repo.moveActiveIssuesToStatus(projectId, statusId, UUID.randomUUID()));
    }
}
