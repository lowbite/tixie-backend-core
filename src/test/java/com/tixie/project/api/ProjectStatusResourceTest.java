package com.tixie.project.api;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.api.dto.CreateProjectStatusRequest;
import com.tixie.project.api.dto.PatchProjectStatusRequest;
import com.tixie.project.api.dto.ReorderProjectStatusesRequest;
import com.tixie.project.domain.ProjectStatusService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectStatusResourceTest {

    @Test
    void crudEndpoints_delegate() {
        var service = mock(ProjectStatusService.class);
        var currentUser = mock(CurrentUser.class);
        var resource = new ProjectStatusResource();
        resource.projectStatusService = service;
        resource.currentUser = currentUser;
        UUID projectId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        var entity = status(projectId);
        when(currentUser.requireProject(projectId)).thenReturn(user(UserRole.ADMIN));

        when(service.list(projectId)).thenReturn(List.of(entity));
        when(service.create(eq(projectId), anyString(), any(), any())).thenReturn(entity);
        when(service.patch(eq(projectId), eq(statusId), any(), any(), any())).thenReturn(entity);

        assertEquals(1, resource.list(projectId).size());

        var create = new CreateProjectStatusRequest();
        create.name = "Doing";
        Response created = resource.create(projectId, create);
        assertEquals(201, created.getStatus());

        var patch = new PatchProjectStatusRequest();
        patch.name = "Done";
        assertNotNull(resource.patch(projectId, statusId, patch));

        var reorder = new ReorderProjectStatusesRequest();
        reorder.statusIds = List.of(statusId);
        assertEquals(1, resource.reorder(projectId, reorder).size());

        assertEquals(204, resource.delete(projectId, statusId, null).getStatus());
        verify(service).delete(projectId, statusId, null);
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

    private UserEntity user(UserRole role) {
        var user = new UserEntity();
        user.id = UUID.randomUUID();
        user.companyId = UUID.randomUUID();
        user.role = role;
        return user;
    }
}
