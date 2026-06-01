package com.tixie.company.api;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.company.CompanyEntity;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import com.tixie.company.domain.CompanyService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyResourceTest {

    @Test
    void list_mapsCurrentUsersCompany() {
        var service = mock(CompanyService.class);
        var currentUser = mock(CurrentUser.class);
        var resource = new CompanyResource();
        resource.companyService = service;
        resource.currentUser = currentUser;
        var entity = entity();
        var user = user(entity.id, UserRole.VIEWER);
        when(currentUser.require()).thenReturn(user);
        when(service.getById(entity.id)).thenReturn(entity);

        assertEquals(1, resource.list().size());
    }

    @Test
    void getUpdateDelete_delegate() {
        var service = mock(CompanyService.class);
        var currentUser = mock(CurrentUser.class);
        var resource = new CompanyResource();
        resource.companyService = service;
        resource.currentUser = currentUser;
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenReturn(entity());
        when(service.update(eq(id), any(UpdateCompanyRequest.class))).thenReturn(entity());

        assertNotNull(resource.getById(id));
        assertNotNull(resource.update(id, new UpdateCompanyRequest()));
        assertEquals(204, resource.delete(id).getStatus());
        verify(service).delete(id);
    }

    private CompanyEntity entity() {
        var e = new CompanyEntity();
        e.id = UUID.randomUUID();
        e.name = "Acme";
        e.createdAt = Instant.now();
        return e;
    }

    private UserEntity user(UUID companyId, UserRole role) {
        var user = new UserEntity();
        user.id = UUID.randomUUID();
        user.companyId = companyId;
        user.role = role;
        return user;
    }
}
