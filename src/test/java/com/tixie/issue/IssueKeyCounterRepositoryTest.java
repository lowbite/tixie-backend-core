package com.tixie.issue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class IssueKeyCounterRepositoryTest {

    @Test
    void methods_delegateToEntityManager() {
        var repo = spy(new IssueKeyCounterRepository());
        EntityManager em = mock(EntityManager.class);
        Query nativeQuery = mock(Query.class);
        doReturn(em).when(repo).getEntityManager();
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(em.createNativeQuery(anyString(), eq(IssueKeyCounterEntity.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(new IssueKeyCounterEntity()));

        repo.ensureExists(UUID.randomUUID());
        assertTrue(repo.findByProjectIdForUpdate(UUID.randomUUID()).isPresent());
    }
}
