package com.tixie.issue.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
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

        var result = issueService.create(PROJECT_ID, req);

        assertEquals(IssuePriority.HIGH, result.priority);
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
        req.title = "Updated Title";
        req.description = "Updated desc";
        req.priority = IssuePriority.CRITICAL;
        req.statusId = UUID.randomUUID();
        req.parentId = UUID.randomUUID();

        var result = issueService.patch(PROJECT_ID, ISSUE_ID, req);

        assertEquals("Updated Title", result.title);
        assertEquals("Updated desc", result.description);
        assertEquals(IssuePriority.CRITICAL, result.priority);
        assertEquals(req.statusId, result.statusId);
        assertEquals(req.parentId, result.parentId);
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
        req.title = "Rejected Title";
        doThrow(new ValidationException("INVALID_PARENT", "bad parent"))
                .when(validator).validatePatch(req, existing);

        assertThrows(ValidationException.class,
                () -> issueService.patch(PROJECT_ID, ISSUE_ID, req));
        assertEquals("Original Title", existing.title);
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
}
