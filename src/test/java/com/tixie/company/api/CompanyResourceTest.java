package com.tixie.company.api;

import com.tixie.company.CompanyEntity;
import com.tixie.company.api.dto.CreateCompanyRequest;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import com.tixie.company.domain.CompanyService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyResourceTest {

    @Test
    void create_returns201WithBody() {
        var service = mock(CompanyService.class);
        var resource = new CompanyResource();
        resource.companyService = service;
        var req = new CreateCompanyRequest();
        req.name = "Acme";
        var entity = entity();
        when(service.create(req)).thenReturn(entity);

        Response response = resource.create(req);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void list_mapsEntities() {
        var service = mock(CompanyService.class);
        var resource = new CompanyResource();
        resource.companyService = service;
        when(service.list()).thenReturn(List.of(entity()));
        assertEquals(1, resource.list().size());
    }

    @Test
    void getUpdateDelete_delegate() {
        var service = mock(CompanyService.class);
        var resource = new CompanyResource();
        resource.companyService = service;
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
}
