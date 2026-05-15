package com.tixie.issue.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class IssueSoftDeleteHandlerTest {

    @Test
    void softDelete_executesQueries() {
        var handler = new IssueSoftDeleteHandler();
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        handler.em = em;
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        handler.softDelete(UUID.randomUUID());
        handler.softDeleteByProjectId(UUID.randomUUID());

        verify(query, times(2)).executeUpdate();
    }
}
