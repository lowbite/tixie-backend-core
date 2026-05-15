package com.tixie.issue.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.MoveIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.tixie.issue.domain.model.IssueType.STORY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID ISSUE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STATUS_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PARENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID OTHER_PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock IssueRepository issueRepository;
    @Mock ProjectRepository projectRepository;
    @Mock ProjectStatusRepository projectStatusRepository;
    @Mock IssueValidator validator;
    @Mock IssueKeyGenerator keyGenerator;
    @Mock IssueSoftDeleteHandler softDeleteHandler;

    @InjectMocks IssueService issueService;

    private ProjectEntity project() {
        var p = new ProjectEntity();
        p.id = PROJECT_ID;
        p.companyId = UUID.randomUUID();
        p.key = "PRJ";
        p.createdAt = Instant.now();
        return p;
    }

    private IssueEntity issue(UUID issueId, UUID projectId) {
        var e = new IssueEntity();
        e.id = issueId;
        e.projectId = projectId;
        e.type = STORY;
        e.issueKey = "PRJ-1";
        e.title = "Original Title";
        e.description = "Original desc";
        e.priority = IssuePriority.LOW;
        e.statusId = STATUS_ID;
        e.createdAt = Instant.now();
        e.updatedAt = Instant.now();
        e.position = 1;
        return e;
    }

    private CreateIssueRequest createReq(IssueType type, IssuePriority priority) {
        var req = new CreateIssueRequest();
        req.title = "New Issue";
        req.description = "desc";
        req.type = type;
        req.priority = priority;
        req.statusId = STATUS_ID;
        req.parentId = PARENT_ID;
        return req;
    }

    // =========================================================
    // create
    // =========================================================

    @Test
    void create_withExplicitPriority_persistsWithThatPriority() {
        var req = createReq(STORY, IssuePriority.HIGH);
        when(keyGenerator.generate(PROJECT_ID)).thenReturn("PRJ-1");
        when(issueRepository.nextPosition(PROJECT_ID, STATUS_ID)).thenReturn(3L);

        var result = issueService.create(PROJECT_ID, req);

        assertEquals(IssuePriority.HIGH, result.priority);
        assertEquals(3L, result.position);
        verify(issueRepository).persist(result);
    }

    @Test
    void create_withNullPriority_defaultsToMedium() {
        var req = createReq(STORY, null);
        when(keyGenerator.generate(PROJECT_ID)).thenReturn("PRJ-1");

        var result = issueService.create(PROJECT_ID, req);

        assertEquals(IssuePriority.MEDIUM, result.priority);
    }

    @Test
    void create_setsAllFieldsFromRequest() {
        var req = createReq(STORY, IssuePriority.CRITICAL);
        when(keyGenerator.generate(PROJECT_ID)).thenReturn("PRJ-42");

        var result = issueService.create(PROJECT_ID, req);

        assertEquals("PRJ-42", result.issueKey);
        assertEquals("New Issue", result.title);
        assertEquals("desc", result.description);
        assertEquals(STORY, result.type);
        assertEquals(STATUS_ID, result.statusId);
        assertEquals(PROJECT_ID, result.projectId);
        assertEquals(PARENT_ID, result.parentId);
    }

    @Test
    void create_setsCreatedAtAndUpdatedAt() {
        var req = createReq(STORY, IssuePriority.MEDIUM);
        when(keyGenerator.generate(PROJECT_ID)).thenReturn("PRJ-1");

        var before = Instant.now();
        var result = issueService.create(PROJECT_ID, req);

        assertNotNull(result.createdAt);
        assertNotNull(result.updatedAt);
        assertFalse(result.createdAt.isBefore(before));
        assertFalse(result.updatedAt.isBefore(before));
    }

    @Test
    void create_delegatesToValidator() {
        var req = createReq(STORY, IssuePriority.MEDIUM);
        when(keyGenerator.generate(PROJECT_ID)).thenReturn("PRJ-1");

        issueService.create(PROJECT_ID, req);

        verify(validator).validateCreate(req, PROJECT_ID);
    }

    @Test
    void create_validationFails_doesNotPersist() {
        var req = createReq(STORY, IssuePriority.MEDIUM);
        doThrow(new ValidationException("INVALID_STATUS", "bad status"))
                .when(validator).validateCreate(req, PROJECT_ID);

        assertThrows(ValidationException.class, () -> issueService.create(PROJECT_ID, req));
        verify(issueRepository, never()).persist(any(IssueEntity.class));
    }

    // =========================================================
    // list
    // =========================================================

    @Test
    void list_activeProject_returnsIssuesFromRepo() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project()));
        var issues = List.of(issue(ISSUE_ID, PROJECT_ID), issue(UUID.randomUUID(), PROJECT_ID));
        when(issueRepository.findActiveByProjectId(PROJECT_ID)).thenReturn(issues);

        var result = issueService.list(PROJECT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void list_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> issueService.list(PROJECT_ID));
        verifyNoInteractions(issueRepository);
    }

    // =========================================================
    // getById
    // =========================================================

    @Test
    void getById_issueFoundWithMatchingProject_returnsIssue() {
        var e = issue(ISSUE_ID, PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(e));

        var result = issueService.getById(PROJECT_ID, ISSUE_ID);

        assertSame(e, result);
    }

    @Test
    void getById_issueNotFound_throwsNotFound() {
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> issueService.getById(PROJECT_ID, ISSUE_ID));
    }

    @Test
    void getById_issueBelongsToDifferentProject_throwsNotFound() {
        var e = issue(ISSUE_ID, OTHER_PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(e));

        assertThrows(NotFoundException.class, () -> issueService.getById(PROJECT_ID, ISSUE_ID));
    }

    // =========================================================
    // patch
    // =========================================================

    @Test
    void patch_allFieldsNonNull_updatesAllFields() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        var req = new PatchIssueRequest();
        req.setTitle("Updated Title");
        req.setDescription("Updated desc");
        req.setPriority(IssuePriority.CRITICAL);
        req.setStatusId(UUID.randomUUID());
        req.setParentId(UUID.randomUUID());

        var result = issueService.patch(PROJECT_ID, ISSUE_ID, req);

        assertEquals("Updated Title", result.title);
        assertEquals("Updated desc", result.description);
        assertEquals(IssuePriority.CRITICAL, result.priority);
        assertEquals(req.getStatusId(), result.statusId);
        assertEquals(req.getParentId(), result.parentId);
    }

    @Test
    void patch_nullFields_keepsOriginalValues() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        var req = new PatchIssueRequest(); // all null

        var result = issueService.patch(PROJECT_ID, ISSUE_ID, req);

        assertEquals("Original Title", result.title);
        assertEquals("Original desc", result.description);
        assertEquals(IssuePriority.LOW, result.priority);
        assertEquals(STATUS_ID, result.statusId);
    }

    @Test
    void patch_alwaysRefreshesUpdatedAt() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        var originalUpdatedAt = existing.updatedAt;
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));

        var before = Instant.now();
        var result = issueService.patch(PROJECT_ID, ISSUE_ID, new PatchIssueRequest());

        assertFalse(result.updatedAt.isBefore(before));
        // updatedAt should be updated even with empty patch
        assertFalse(result.updatedAt.isBefore(originalUpdatedAt));
    }

    @Test
    void patch_delegatesToValidator() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        var req = new PatchIssueRequest();

        issueService.patch(PROJECT_ID, ISSUE_ID, req);

        verify(validator).validatePatch(req, existing);
    }

    @Test
    void patch_issueNotFound_throwsNotFound_andValidatorNotCalled() {
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> issueService.patch(PROJECT_ID, ISSUE_ID, new PatchIssueRequest()));
        verifyNoInteractions(validator);
    }

    @Test
    void patch_validationFails_fieldsNotMutated() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        var req = new PatchIssueRequest();
        req.setTitle("Rejected Title");
        doThrow(new ValidationException("INVALID_PARENT", "bad parent"))
                .when(validator).validatePatch(req, existing);

        assertThrows(ValidationException.class,
                () -> issueService.patch(PROJECT_ID, ISSUE_ID, req));
        assertEquals("Original Title", existing.title);
    }

    @Test
    void patch_explicitNullDescriptionAndParent_clearsBoth() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        existing.description = "Has description";
        existing.parentId = PARENT_ID;
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        var req = new PatchIssueRequest();
        req.setDescription(null);
        req.setParentId(null);

        var result = issueService.patch(PROJECT_ID, ISSUE_ID, req);

        assertNull(result.description);
        assertNull(result.parentId);
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_issueFound_callsSoftDeleteHandler() {
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(issue(ISSUE_ID, PROJECT_ID)));

        issueService.delete(PROJECT_ID, ISSUE_ID);

        verify(softDeleteHandler).softDelete(ISSUE_ID);
    }

    @Test
    void delete_issueNotFound_throwsNotFound_andHandlerNotCalled() {
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> issueService.delete(PROJECT_ID, ISSUE_ID));
        verifyNoInteractions(softDeleteHandler);
    }

    // =========================================================
    // transition
    // =========================================================

    @Test
    void transition_valid_updatesStatusAndUpdatedAt() {
        var existing = issue(ISSUE_ID, PROJECT_ID);
        UUID targetStatus = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(existing));
        when(issueRepository.nextPosition(PROJECT_ID, targetStatus)).thenReturn(4L);

        var before = Instant.now();
        var result = issueService.transition(PROJECT_ID, ISSUE_ID, targetStatus);

        assertEquals(targetStatus, result.statusId);
        assertEquals(4L, result.position);
        assertFalse(result.updatedAt.isBefore(before));
        verify(validator).validateTransition(targetStatus, existing);
    }

    @Test
    void transition_issueNotFound_throwsNotFound() {
        UUID targetStatus = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> issueService.transition(PROJECT_ID, ISSUE_ID, targetStatus));
        verify(validator, never()).validateTransition(any(), any());
    }

    @Test
    void move_acrossStatuses_reindexesSourceAndTarget() {
        var moving = issue(ISSUE_ID, PROJECT_ID);
        moving.statusId = STATUS_ID;
        moving.position = 1;
        UUID targetStatus = UUID.fromString("00000000-0000-0000-0000-000000000099");

        var sourceOther = issue(UUID.randomUUID(), PROJECT_ID);
        sourceOther.statusId = STATUS_ID;
        sourceOther.position = 2;

        var targetA = issue(UUID.randomUUID(), PROJECT_ID);
        targetA.statusId = targetStatus;
        targetA.position = 1;
        var targetB = issue(UUID.randomUUID(), PROJECT_ID);
        targetB.statusId = targetStatus;
        targetB.position = 2;

        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(moving));
        when(issueRepository.findActiveByProjectIdAndStatusId(PROJECT_ID, targetStatus)).thenReturn(List.of(targetA, targetB));
        when(issueRepository.findActiveByProjectIdAndStatusId(PROJECT_ID, STATUS_ID)).thenReturn(List.of(moving, sourceOther));

        var req = new MoveIssueRequest();
        req.targetStatusId = targetStatus;
        req.targetPosition = 1;

        var result = issueService.move(PROJECT_ID, ISSUE_ID, req);

        assertEquals(targetStatus, result.statusId);
        assertEquals(1L, moving.position);
        assertEquals(2L, targetA.position);
        assertEquals(3L, targetB.position);
        assertEquals(1L, sourceOther.position);
        verify(validator).validateStatusBelongsToProject(targetStatus, PROJECT_ID);
    }

    @Test
    void move_withinSameStatus_reordersIssues() {
        var moving = issue(ISSUE_ID, PROJECT_ID);
        moving.statusId = STATUS_ID;
        moving.position = 1;
        var a = issue(UUID.randomUUID(), PROJECT_ID);
        a.statusId = STATUS_ID;
        a.position = 2;
        var b = issue(UUID.randomUUID(), PROJECT_ID);
        b.statusId = STATUS_ID;
        b.position = 3;

        when(issueRepository.findActiveById(ISSUE_ID)).thenReturn(Optional.of(moving));
        when(issueRepository.findActiveByProjectIdAndStatusId(PROJECT_ID, STATUS_ID)).thenReturn(List.of(moving, a, b));

        var req = new MoveIssueRequest();
        req.targetStatusId = STATUS_ID;
        req.targetPosition = 3;

        issueService.move(PROJECT_ID, ISSUE_ID, req);

        assertEquals(1L, a.position);
        assertEquals(2L, b.position);
        assertEquals(3L, moving.position);
    }

    @Test
    void board_groupsIssuesByStatusAndSortsByPosition() {
        var project = project();
        var todo = new ProjectStatusEntity();
        todo.id = UUID.fromString("00000000-0000-0000-0000-000000000090");
        todo.projectId = PROJECT_ID;
        todo.name = "To Do";
        todo.displayOrder = 1;
        todo.isDefault = true;
        var done = new ProjectStatusEntity();
        done.id = UUID.fromString("00000000-0000-0000-0000-000000000091");
        done.projectId = PROJECT_ID;
        done.name = "Done";
        done.displayOrder = 2;
        done.isDefault = false;

        var i1 = issue(UUID.randomUUID(), PROJECT_ID);
        i1.statusId = done.id;
        i1.position = 2;
        var i2 = issue(UUID.randomUUID(), PROJECT_ID);
        i2.statusId = done.id;
        i2.position = 1;

        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectStatusRepository.findActiveByProjectId(PROJECT_ID)).thenReturn(List.of(done, todo));
        when(issueRepository.findActiveByProjectId(PROJECT_ID)).thenReturn(List.of(i1, i2));

        var board = issueService.board(PROJECT_ID);

        assertEquals(PROJECT_ID, board.projectId);
        assertEquals("To Do", board.columns.get(0).statusName());
        assertEquals("Done", board.columns.get(1).statusName());
        assertEquals(i2.id, board.columns.get(1).issues().get(0).id());
        assertEquals(i1.id, board.columns.get(1).issues().get(1).id());
    }
}
