package com.tixie.project;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ProjectStatusRepositoryTest {

    @Test
    void methods_delegateToPanache() {
        var repo = spy(new ProjectStatusRepository());
        @SuppressWarnings("unchecked")
        PanacheQuery<ProjectStatusEntity> query = mock(PanacheQuery.class);
        var entity = new ProjectStatusEntity();
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        doReturn(query).when(repo).find("id = ?1 and projectId = ?2 and deletedAt is null", id, projectId);
        when(query.firstResultOptional()).thenReturn(Optional.of(entity));
        doReturn(1L).when(repo).count("projectId = ?1 and lower(name) = lower(?2) and deletedAt is null", projectId, "To Do");
        doReturn(2L).when(repo).count(
                "projectId = ?1 and id <> ?2 and lower(name) = lower(?3) and deletedAt is null",
                projectId, id, "To Do");
        doReturn(1).when(repo).update("deletedAt = CURRENT_TIMESTAMP where projectId = ?1 and deletedAt is null", projectId);

        assertTrue(repo.findActiveByIdAndProjectId(id, projectId).isPresent());
        assertTrue(repo.existsActiveByName(projectId, "To Do"));
        assertTrue(repo.existsActiveByNameExcludingId(projectId, id, "To Do"));
        repo.softDeleteActiveByProjectId(projectId);
    }
}
