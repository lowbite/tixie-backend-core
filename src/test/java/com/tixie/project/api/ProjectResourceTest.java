package com.tixie.project.api;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.AuthorizationService;
import com.tixie.authz.Permission;
import com.tixie.authz.ResourceType;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.api.dto.CreateProjectRequest;
import com.tixie.project.api.dto.UpdateProjectRequest;
import com.tixie.project.domain.ProjectService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectResourceTest {

    @Test
    void createAndList_work() {
        var service = mock(ProjectService.class);
        var currentUser = mock(CurrentUser.class);
        var authorizationService = mock(AuthorizationService.class);
        var resource = new ProjectResource();
        resource.projectService = service;
        resource.currentUser = currentUser;
        resource.authorizationService = authorizationService;
        UUID companyId = UUID.randomUUID();
        var user = user(companyId, UserRole.ADMIN);
        var req = new CreateProjectRequest();
        req.name = "P";
        req.key = "PR";
        var project = project(companyId);
        when(currentUser.require()).thenReturn(user);
        when(authorizationService.can(eq(user), eq(Permission.PROJECT_READ), eq(ResourceType.PROJECT), any()))
                .thenReturn(true);
        when(service.create(companyId, req)).thenReturn(project);
        when(service.getStatuses(project.id)).thenReturn(List.of(status(project.id)));
        when(service.list(companyId)).thenReturn(List.of(project));

        Response created = resource.create(companyId, req);
        assertEquals(201, created.getStatus());
        assertEquals(1, resource.list(companyId).size());
    }

    @Test
    void getUpdateDelete_delegate() {
        var service = mock(ProjectService.class);
        var currentUser = mock(CurrentUser.class);
        var authorizationService = mock(AuthorizationService.class);
        var resource = new ProjectResource();
        resource.projectService = service;
        resource.currentUser = currentUser;
        resource.authorizationService = authorizationService;
        UUID companyId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        var project = project(companyId);
        var user = user(companyId, UserRole.OWNER);
        when(currentUser.require()).thenReturn(user);
        when(authorizationService.can(eq(user), eq(Permission.PROJECT_READ), eq(ResourceType.PROJECT), any()))
                .thenReturn(true);
        when(service.getById(companyId, projectId)).thenReturn(project);
        when(service.getStatuses(project.id)).thenReturn(List.of());
        when(service.update(eq(companyId), eq(projectId), any(UpdateProjectRequest.class))).thenReturn(project);

        assertNotNull(resource.getById(companyId, projectId));
        assertNotNull(resource.update(companyId, projectId, new UpdateProjectRequest()));
        assertEquals(204, resource.delete(companyId, projectId).getStatus());
        verify(service).delete(companyId, projectId);
    }

    private UserEntity user(UUID companyId, UserRole role) {
        var user = new UserEntity();
        user.id = UUID.randomUUID();
        user.companyId = companyId;
        user.role = role;
        return user;
    }

    private ProjectEntity project(UUID companyId) {
        var p = new ProjectEntity();
        p.id = UUID.randomUUID();
        p.companyId = companyId;
        p.name = "Project";
        p.key = "PROJ";
        p.createdAt = Instant.now();
        return p;
    }

    private ProjectStatusEntity status(UUID projectId) {
        var s = new ProjectStatusEntity();
        s.id = UUID.randomUUID();
        s.projectId = projectId;
        s.name = "To Do";
        s.displayOrder = 1;
        s.isDefault = true;
        return s;
    }
}
