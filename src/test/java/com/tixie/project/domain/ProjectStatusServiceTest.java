package com.tixie.project.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.issue.IssueRepository;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectStatusServiceTest {

    static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID STATUS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock ProjectService projectService;
    @Mock ProjectStatusRepository projectStatusRepository;
    @Mock IssueRepository issueRepository;

    @InjectMocks ProjectStatusService service;

    private ProjectStatusEntity status(UUID id, String name, int order, boolean isDefault) {
        var s = new ProjectStatusEntity();
        s.id = id;
        s.projectId = PROJECT_ID;
        s.name = name;
        s.displayOrder = order;
        s.isDefault = isDefault;
        return s;
    }

    @Test
    void create_duplicateName_throwsValidation() {
        when(projectStatusRepository.existsActiveByName(PROJECT_ID, "Done")).thenReturn(true);

        var ex = assertThrows(ValidationException.class, () -> service.create(PROJECT_ID, "Done", null, false));
        assertEquals("DUPLICATE_STATUS_NAME", ex.getCode());
    }

    @Test
    void patch_setDefault_unsetsExistingDefault() {
        var existingDefault = status(UUID.randomUUID(), "To Do", 1, true);
        var patched = status(STATUS_ID, "Doing", 2, false);
        when(projectStatusRepository.findActiveByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.of(patched));
        when(projectStatusRepository.findActiveDefaultByProjectId(PROJECT_ID)).thenReturn(Optional.of(existingDefault));

        var result = service.patch(PROJECT_ID, STATUS_ID, null, null, true);

        assertTrue(result.isDefault);
        assertFalse(existingDefault.isDefault);
    }

    @Test
    void reorder_invalidPayload_throwsValidation() {
        var statuses = List.of(status(STATUS_ID, "A", 1, true), status(TARGET_ID, "B", 2, false));
        when(projectStatusRepository.findActiveByProjectId(PROJECT_ID)).thenReturn(statuses);

        var ex = assertThrows(ValidationException.class, () -> service.reorder(PROJECT_ID, List.of(STATUS_ID)));
        assertEquals("INVALID_REORDER", ex.getCode());
    }

    @Test
    void delete_statusWithIssuesWithoutTarget_throwsValidation() {
        var deleting = status(STATUS_ID, "In Progress", 2, false);
        when(projectStatusRepository.findActiveByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.of(deleting));
        when(projectStatusRepository.findActiveByProjectId(PROJECT_ID))
                .thenReturn(List.of(status(UUID.randomUUID(), "To Do", 1, true), deleting));
        when(issueRepository.countActiveByProjectIdAndStatusId(PROJECT_ID, STATUS_ID)).thenReturn(5L);

        var ex = assertThrows(ValidationException.class, () -> service.delete(PROJECT_ID, STATUS_ID, null));
        assertEquals("STATUS_IN_USE", ex.getCode());
    }

    @Test
    void delete_withMoveTarget_movesIssuesAndSoftDeletesStatus() {
        var deleting = status(STATUS_ID, "In Progress", 2, false);
        var target = status(TARGET_ID, "Done", 3, false);
        when(projectStatusRepository.findActiveByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.of(deleting));
        when(projectStatusRepository.findActiveByIdAndProjectId(TARGET_ID, PROJECT_ID)).thenReturn(Optional.of(target));
        when(projectStatusRepository.findActiveByProjectId(PROJECT_ID))
                .thenReturn(List.of(status(UUID.randomUUID(), "To Do", 1, true), deleting, target));
        when(issueRepository.countActiveByProjectIdAndStatusId(PROJECT_ID, STATUS_ID)).thenReturn(2L);

        service.delete(PROJECT_ID, STATUS_ID, TARGET_ID);

        assertNotNull(deleting.deletedAt);
        verify(issueRepository).moveActiveIssuesToStatus(PROJECT_ID, STATUS_ID, TARGET_ID);
    }
}
