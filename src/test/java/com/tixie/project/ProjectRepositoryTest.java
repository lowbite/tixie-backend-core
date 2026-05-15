package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectRepositoryTest {

    @Test
    void methods_delegateToPanache() {
        var repo = spy(new ProjectRepository());
        @SuppressWarnings("unchecked")
        PanacheQuery<ProjectEntity> query = mock(PanacheQuery.class);
        var entity = new ProjectEntity();
        UUID id = UUID.randomUUID();
        doReturn(query).when(repo).find("id = ?1 and deletedAt is null", id);
        when(query.firstResultOptional()).thenReturn(Optional.of(entity));
        doReturn(1L).when(repo).count("key = ?1", "PROJ");

        assertTrue(repo.findActiveById(id).isPresent());
        assertTrue(repo.existsByKey("PROJ"));
    }
}
