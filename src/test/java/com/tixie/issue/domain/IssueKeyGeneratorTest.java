package com.tixie.issue.domain;

import com.tixie.issue.IssueKeyCounterEntity;
import com.tixie.issue.IssueKeyCounterRepository;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueKeyGeneratorTest {

    static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock ProjectRepository projectRepository;
    @Mock IssueKeyCounterRepository counterRepository;

    @InjectMocks IssueKeyGenerator keyGenerator;

    private ProjectEntity project(String key) {
        var p = new ProjectEntity();
        p.id = PROJECT_ID;
        p.companyId = UUID.randomUUID();
        p.key = key;
        p.createdAt = Instant.now();
        return p;
    }

    private IssueKeyCounterEntity counter(long lastSeq) {
        var c = new IssueKeyCounterEntity();
        c.projectId = PROJECT_ID;
        c.lastSeq = lastSeq;
        return c;
    }

    @Test
    void generate_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> keyGenerator.generate(PROJECT_ID));
    }

    @Test
    void generate_existingCounter_incrementsSeqAndReturnsKey() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project("PROJ")));
        var existingCounter = counter(5L);
        when(counterRepository.findByProjectIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(existingCounter));

        String result = keyGenerator.generate(PROJECT_ID);

        assertEquals("PROJ-6", result);
        assertEquals(6L, existingCounter.lastSeq);
    }

    @Test
    void generate_initialCounter_incrementsFromZeroAndReturnsKey() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project("PROJ")));
        var initializedCounter = counter(0L);
        when(counterRepository.findByProjectIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(initializedCounter));

        String result = keyGenerator.generate(PROJECT_ID);

        assertEquals("PROJ-1", result);
        assertEquals(1L, initializedCounter.lastSeq);
        verify(counterRepository).ensureExists(PROJECT_ID);
    }

    @Test
    void generate_keyUsesProjectPrefix() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project("WEB")));
        when(counterRepository.findByProjectIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(counter(99L)));

        String result = keyGenerator.generate(PROJECT_ID);

        assertEquals("WEB-100", result);
    }

    @Test
    void generate_counterStillMissingAfterInitialization_throwsIllegalState() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project("PROJ")));
        when(counterRepository.findByProjectIdForUpdate(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> keyGenerator.generate(PROJECT_ID));
    }
}
