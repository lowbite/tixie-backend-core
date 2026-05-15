package com.tixie.issue.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.CreateIssueRequest;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.tixie.issue.domain.model.IssueType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueValidatorTest {

    static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STATUS_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PARENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID OTHER_PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock ProjectRepository projectRepository;
    @Mock CompanyRepository companyRepository;
    @Mock ProjectStatusRepository projectStatusRepository;
    @Mock IssueRepository issueRepository;

    @InjectMocks IssueValidator validator;

    // --- helpers ---

    private ProjectEntity project() {
        var p = new ProjectEntity();
        p.id = PROJECT_ID;
        p.companyId = COMPANY_ID;
        p.key = "PRJ";
        p.createdAt = Instant.now();
        return p;
    }

    private CompanyEntity company() {
        var c = new CompanyEntity();
        c.id = COMPANY_ID;
        c.name = "Acme";
        c.createdAt = Instant.now();
        return c;
    }

    private ProjectStatusEntity status() {
        var s = new ProjectStatusEntity();
        s.id = STATUS_ID;
        s.projectId = PROJECT_ID;
        s.name = "To Do";
        return s;
    }

    private IssueEntity issue(UUID id, UUID projectId, IssueType type) {
        var e = new IssueEntity();
        e.id = id;
        e.projectId = projectId;
        e.type = type;
        e.issueKey = "PRJ-1";
        e.title = "Issue";
        e.priority = IssuePriority.MEDIUM;
        e.statusId = STATUS_ID;
        e.createdAt = Instant.now();
        e.updatedAt = Instant.now();
        return e;
    }

    private CreateIssueRequest createReq(IssueType type, UUID parentId) {
        var req = new CreateIssueRequest();
        req.title = "My Issue";
        req.type = type;
        req.statusId = STATUS_ID;
        req.parentId = parentId;
        return req;
    }

    private void stubHappyPath() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(projectStatusRepository.findByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.of(status()));
    }

    // =========================================================
    // validateCreate — happy paths
    // =========================================================

    @Test
    void validateCreate_epic_withoutParent_passes() {
        stubHappyPath();
        assertDoesNotThrow(() -> validator.validateCreate(createReq(EPIC, null), PROJECT_ID));
    }

    @Test
    void validateCreate_story_withEpicParent_passes() {
        stubHappyPath();
        var parent = issue(PARENT_ID, PROJECT_ID, EPIC);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        assertDoesNotThrow(() -> validator.validateCreate(createReq(STORY, PARENT_ID), PROJECT_ID));
    }

    @Test
    void validateCreate_task_withStoryParent_passes() {
        stubHappyPath();
        var parent = issue(PARENT_ID, PROJECT_ID, STORY);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        assertDoesNotThrow(() -> validator.validateCreate(createReq(TASK, PARENT_ID), PROJECT_ID));
    }

    @Test
    void validateCreate_subtask_withTaskParent_passes() {
        stubHappyPath();
        var parent = issue(PARENT_ID, PROJECT_ID, TASK);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        assertDoesNotThrow(() -> validator.validateCreate(createReq(SUBTASK, PARENT_ID), PROJECT_ID));
    }

    // =========================================================
    // validateCreate — failure paths
    // =========================================================

    @Test
    void validateCreate_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> validator.validateCreate(createReq(EPIC, null), PROJECT_ID));
    }

    @Test
    void validateCreate_companyNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> validator.validateCreate(createReq(EPIC, null), PROJECT_ID));
    }

    @Test
    void validateCreate_statusNotInProject_throwsValidationWithInvalidStatusCode() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(projectStatusRepository.findByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq(EPIC, null), PROJECT_ID));
        assertEquals("INVALID_STATUS", ex.getCode());
    }

    @Test
    void validateCreate_epicWithParent_throwsValidationWithInvalidParentCode_andIssueRepoNotCalled() {
        stubHappyPath();

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq(EPIC, PARENT_ID), PROJECT_ID));
        assertEquals("INVALID_PARENT", ex.getCode());
        verifyNoInteractions(issueRepository);
    }

    @Test
    void validateCreate_parentNotFound_throwsNotFound() {
        stubHappyPath();
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> validator.validateCreate(createReq(STORY, PARENT_ID), PROJECT_ID));
    }

    @Test
    void validateCreate_parentInDifferentProject_throwsValidationWithInvalidParentCode() {
        stubHappyPath();
        var parent = issue(PARENT_ID, OTHER_PROJECT_ID, EPIC);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq(STORY, PARENT_ID), PROJECT_ID));
        assertEquals("INVALID_PARENT", ex.getCode());
    }

    @Test
    void validateCreate_wrongParentType_storyUnderTask_throwsValidation() {
        stubHappyPath();
        var parent = issue(PARENT_ID, PROJECT_ID, TASK); // STORY requires EPIC parent
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq(STORY, PARENT_ID), PROJECT_ID));
        assertEquals("INVALID_PARENT", ex.getCode());
    }

    @Test
    void validateCreate_wrongParentType_taskUnderEpic_throwsValidation() {
        stubHappyPath();
        var parent = issue(PARENT_ID, PROJECT_ID, EPIC); // TASK requires STORY parent
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq(TASK, PARENT_ID), PROJECT_ID));
        assertEquals("INVALID_PARENT", ex.getCode());
    }

    // =========================================================
    // validatePatch — happy paths
    // =========================================================

    @Test
    void validatePatch_allNullFields_noInteractionsWithRepos() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, STORY);
        var req = new PatchIssueRequest();

        assertDoesNotThrow(() -> validator.validatePatch(req, existing));
        verifyNoInteractions(projectStatusRepository, issueRepository);
    }

    @Test
    void validatePatch_validStatusChange_passes() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, TASK);
        var req = new PatchIssueRequest();
        req.statusId = STATUS_ID;
        when(projectStatusRepository.findByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.of(status()));

        assertDoesNotThrow(() -> validator.validatePatch(req, existing));
    }

    @Test
    void validatePatch_story_withEpicParent_passes() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, STORY);
        var req = new PatchIssueRequest();
        req.parentId = PARENT_ID;
        var parent = issue(PARENT_ID, PROJECT_ID, EPIC);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        assertDoesNotThrow(() -> validator.validatePatch(req, existing));
    }

    // =========================================================
    // validatePatch — failure paths
    // =========================================================

    @Test
    void validatePatch_typeProvided_throwsValidation() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, STORY);
        var req = new PatchIssueRequest();
        req.type = TASK;

        var ex = assertThrows(ValidationException.class, () -> validator.validatePatch(req, existing));
        assertEquals("IMMUTABLE_TYPE", ex.getCode());
        verifyNoInteractions(projectStatusRepository, issueRepository);
    }

    @Test
    void validatePatch_statusNotInProject_throwsValidation() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, TASK);
        var req = new PatchIssueRequest();
        req.statusId = STATUS_ID;
        when(projectStatusRepository.findByIdAndProjectId(STATUS_ID, PROJECT_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(ValidationException.class, () -> validator.validatePatch(req, existing));
        assertEquals("INVALID_STATUS", ex.getCode());
    }

    @Test
    void validatePatch_epicWithNewParent_throwsValidation_andIssueRepoNotCalled() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, EPIC);
        var req = new PatchIssueRequest();
        req.parentId = PARENT_ID;

        var ex = assertThrows(ValidationException.class, () -> validator.validatePatch(req, existing));
        assertEquals("INVALID_PARENT", ex.getCode());
        verifyNoInteractions(issueRepository);
    }

    @Test
    void validatePatch_parentNotFound_throwsNotFound() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, STORY);
        var req = new PatchIssueRequest();
        req.parentId = PARENT_ID;
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> validator.validatePatch(req, existing));
    }

    @Test
    void validatePatch_parentInDifferentProject_throwsValidation() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, STORY);
        var req = new PatchIssueRequest();
        req.parentId = PARENT_ID;
        var parent = issue(PARENT_ID, OTHER_PROJECT_ID, EPIC);
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        var ex = assertThrows(ValidationException.class, () -> validator.validatePatch(req, existing));
        assertEquals("INVALID_PARENT", ex.getCode());
    }

    @Test
    void validatePatch_wrongParentType_throwsValidation() {
        var existing = issue(UUID.randomUUID(), PROJECT_ID, TASK);
        var req = new PatchIssueRequest();
        req.parentId = PARENT_ID;
        var parent = issue(PARENT_ID, PROJECT_ID, EPIC); // TASK requires STORY parent
        when(issueRepository.findActiveById(PARENT_ID)).thenReturn(Optional.of(parent));

        var ex = assertThrows(ValidationException.class, () -> validator.validatePatch(req, existing));
        assertEquals("INVALID_PARENT", ex.getCode());
    }
}
