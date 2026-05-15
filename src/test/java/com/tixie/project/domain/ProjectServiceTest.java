package com.tixie.project.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import com.tixie.issue.domain.IssueSoftDeleteHandler;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import com.tixie.project.api.dto.CreateProjectRequest;
import com.tixie.project.api.dto.UpdateProjectRequest;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    static final UUID COMPANY_ID       = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID PROJECT_ID       = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID OTHER_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock ProjectRepository        projectRepository;
    @Mock ProjectStatusRepository  projectStatusRepository;
    @Mock CompanyRepository        companyRepository;
    @Mock ProjectValidator         validator;
    @Mock IssueSoftDeleteHandler   issueSoftDeleteHandler;

    @InjectMocks ProjectService projectService;

    @BeforeEach
    void assignGeneratedProjectId() {
        lenient().doAnswer(invocation -> {
            var project = invocation.getArgument(0, ProjectEntity.class);
            project.id = PROJECT_ID;
            return null;
        }).when(projectRepository).persist(any(ProjectEntity.class));
    }

    private CompanyEntity company() {
        var c = new CompanyEntity();
        c.id = COMPANY_ID;
        c.name = "Acme Corp";
        c.createdAt = Instant.now();
        return c;
    }

    private ProjectEntity project(UUID companyId) {
        var p = new ProjectEntity();
        p.id = PROJECT_ID;
        p.companyId = companyId;
        p.name = "Original Name";
        p.key = "PROJ";
        p.createdAt = Instant.now();
        return p;
    }

    private CreateProjectRequest createReq(String name, String key) {
        var req = new CreateProjectRequest();
        req.name = name;
        req.key = key;
        return req;
    }

    // =========================================================
    // create
    // =========================================================

    @Test
    void create_setsAllFieldsFromRequest() {
        var req = createReq("My Project", "MYPR");

        var result = projectService.create(COMPANY_ID, req);

        assertEquals("My Project", result.name);
        assertEquals("MYPR", result.key);
        assertEquals(COMPANY_ID, result.companyId);
    }

    @Test
    void create_setsCreatedAtToNow() {
        var before = Instant.now();

        var result = projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        assertNotNull(result.createdAt);
        assertFalse(result.createdAt.isBefore(before));
    }

    @Test
    void create_persistsProjectEntity() {
        projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        var captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).persist(captor.capture());
        assertEquals("MYPR", captor.getValue().key);
        assertEquals(COMPANY_ID, captor.getValue().companyId);
    }

    @Test
    void create_seedsThreeDefaultStatuses() {
        projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        verify(projectStatusRepository, times(3)).persist(any(ProjectStatusEntity.class));
    }

    @Test
    void create_defaultStatuses_haveCorrectNamesAndOrder() {
        projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        var captor = ArgumentCaptor.forClass(ProjectStatusEntity.class);
        verify(projectStatusRepository, times(3)).persist(captor.capture());
        var statuses = captor.getAllValues();

        assertEquals("To Do",       statuses.get(0).name);
        assertEquals("In Progress", statuses.get(1).name);
        assertEquals("Done",        statuses.get(2).name);

        assertEquals(1, statuses.get(0).displayOrder);
        assertEquals(2, statuses.get(1).displayOrder);
        assertEquals(3, statuses.get(2).displayOrder);
    }

    @Test
    void create_defaultStatuses_exactlyOneIsDefault() {
        projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        var captor = ArgumentCaptor.forClass(ProjectStatusEntity.class);
        verify(projectStatusRepository, times(3)).persist(captor.capture());
        var defaults = captor.getAllValues().stream().filter(s -> s.isDefault).toList();

        assertEquals(1, defaults.size());
        assertEquals("To Do", defaults.get(0).name);
    }

    @Test
    void create_defaultStatuses_allBelongToNewProject() {
        projectService.create(COMPANY_ID, createReq("My Project", "MYPR"));

        var captor = ArgumentCaptor.forClass(ProjectStatusEntity.class);
        verify(projectStatusRepository, times(3)).persist(captor.capture());

        captor.getAllValues().forEach(s -> assertNotNull(s.projectId));
        var distinctProjects = captor.getAllValues().stream()
                .map(s -> s.projectId).distinct().toList();
        assertEquals(1, distinctProjects.size());
    }

    @Test
    void create_delegatesToValidator() {
        var req = createReq("My Project", "MYPR");

        projectService.create(COMPANY_ID, req);

        verify(validator).validateCreate(req, COMPANY_ID);
    }

    @Test
    void create_validationFails_doesNotPersistProject() {
        var req = createReq("My Project", "MYPR");
        doThrow(new ValidationException("DUPLICATE_KEY", "key in use"))
                .when(validator).validateCreate(req, COMPANY_ID);

        assertThrows(ValidationException.class, () -> projectService.create(COMPANY_ID, req));
        verify(projectRepository, never()).persist(any(ProjectEntity.class));
        verifyNoInteractions(projectStatusRepository);
    }

    // =========================================================
    // list
    // =========================================================

    @Test
    void list_companyFound_returnsProjectsFromRepository() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        var projects = List.of(project(COMPANY_ID), project(COMPANY_ID));
        when(projectRepository.findActiveByCompanyId(COMPANY_ID)).thenReturn(projects);

        var result = projectService.list(COMPANY_ID);

        assertEquals(2, result.size());
    }

    @Test
    void list_companyNotFound_throwsNotFound_andRepoNotCalled() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.list(COMPANY_ID));
        verifyNoInteractions(projectRepository);
    }

    // =========================================================
    // getById
    // =========================================================

    @Test
    void getById_foundWithMatchingCompany_returnsProject() {
        var entity = project(COMPANY_ID);
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(entity));

        var result = projectService.getById(COMPANY_ID, PROJECT_ID);

        assertSame(entity, result);
    }

    @Test
    void getById_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> projectService.getById(COMPANY_ID, PROJECT_ID));
    }

    @Test
    void getById_projectBelongsToDifferentCompany_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID))
                .thenReturn(Optional.of(project(OTHER_COMPANY_ID)));

        assertThrows(NotFoundException.class,
                () -> projectService.getById(COMPANY_ID, PROJECT_ID));
    }

    // =========================================================
    // update
    // =========================================================

    @Test
    void update_nonNullName_updatesName() {
        when(projectRepository.findActiveById(PROJECT_ID))
                .thenReturn(Optional.of(project(COMPANY_ID)));
        var req = new UpdateProjectRequest();
        req.name = "Renamed Project";

        var result = projectService.update(COMPANY_ID, PROJECT_ID, req);

        assertEquals("Renamed Project", result.name);
    }

    @Test
    void update_nullName_keepsOriginalName() {
        when(projectRepository.findActiveById(PROJECT_ID))
                .thenReturn(Optional.of(project(COMPANY_ID)));
        var req = new UpdateProjectRequest(); // name is null

        var result = projectService.update(COMPANY_ID, PROJECT_ID, req);

        assertEquals("Original Name", result.name);
    }

    @Test
    void update_keyIsNeverChanged() {
        when(projectRepository.findActiveById(PROJECT_ID))
                .thenReturn(Optional.of(project(COMPANY_ID)));
        var req = new UpdateProjectRequest();
        req.name = "Renamed";

        var result = projectService.update(COMPANY_ID, PROJECT_ID, req);

        assertEquals("PROJ", result.key);
    }

    @Test
    void update_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> projectService.update(COMPANY_ID, PROJECT_ID, new UpdateProjectRequest()));
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_found_setsDeletedAt() {
        var entity = project(COMPANY_ID);
        assertNull(entity.deletedAt);
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.of(entity));

        var before = Instant.now();
        projectService.delete(COMPANY_ID, PROJECT_ID);

        assertNotNull(entity.deletedAt);
        assertFalse(entity.deletedAt.isBefore(before));
        verify(issueSoftDeleteHandler).softDeleteByProjectId(PROJECT_ID);
        verify(projectStatusRepository).softDeleteActiveByProjectId(PROJECT_ID);
    }

    @Test
    void delete_projectNotFound_throwsNotFound() {
        when(projectRepository.findActiveById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> projectService.delete(COMPANY_ID, PROJECT_ID));
    }

    // =========================================================
    // getStatuses
    // =========================================================

    @Test
    void getStatuses_delegatesToRepository() {
        var statuses = List.of(new ProjectStatusEntity(), new ProjectStatusEntity());
        when(projectStatusRepository.findActiveByProjectId(PROJECT_ID)).thenReturn(statuses);

        var result = projectService.getStatuses(PROJECT_ID);

        assertSame(statuses, result);
        verify(projectStatusRepository).findActiveByProjectId(PROJECT_ID);
    }
}
