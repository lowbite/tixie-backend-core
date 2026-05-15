package com.tixie.company.domain;

import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import com.tixie.company.api.dto.CreateCompanyRequest;
import com.tixie.company.api.dto.UpdateCompanyRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock CompanyRepository companyRepository;

    @InjectMocks CompanyService companyService;

    private CompanyEntity company() {
        var c = new CompanyEntity();
        c.id = COMPANY_ID;
        c.name = "Acme Corp";
        c.createdAt = Instant.now();
        return c;
    }

    private CreateCompanyRequest createReq(String name) {
        var req = new CreateCompanyRequest();
        req.name = name;
        return req;
    }

    // =========================================================
    // create
    // =========================================================

    @Test
    void create_setsNameFromRequest() {
        var result = companyService.create(createReq("New Co"));

        assertEquals("New Co", result.name);
    }

    @Test
    void create_setsCreatedAtToNow() {
        var before = Instant.now();

        var result = companyService.create(createReq("New Co"));

        assertNotNull(result.createdAt);
        assertFalse(result.createdAt.isBefore(before));
    }

    @Test
    void create_persistsEntity() {
        companyService.create(createReq("New Co"));

        var captor = ArgumentCaptor.forClass(CompanyEntity.class);
        verify(companyRepository).persist(captor.capture());
        assertEquals("New Co", captor.getValue().name);
    }

    @Test
    void create_returnsPersistedEntity() {
        var result = companyService.create(createReq("New Co"));

        assertNotNull(result);
        assertEquals("New Co", result.name);
    }

    // =========================================================
    // list
    // =========================================================

    @Test
    void list_returnsActiveCompaniesFromRepository() {
        var companies = List.of(company(), company());
        when(companyRepository.list("deletedAt is null")).thenReturn(companies);

        var result = companyService.list();

        assertEquals(2, result.size());
    }

    @Test
    void list_empty_returnsEmptyList() {
        when(companyRepository.list("deletedAt is null")).thenReturn(List.of());

        var result = companyService.list();

        assertTrue(result.isEmpty());
    }

    // =========================================================
    // getById
    // =========================================================

    @Test
    void getById_found_returnsEntity() {
        var entity = company();
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(entity));

        var result = companyService.getById(COMPANY_ID);

        assertSame(entity, result);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> companyService.getById(COMPANY_ID));
    }

    // =========================================================
    // update
    // =========================================================

    @Test
    void update_nonNullName_updatesName() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        var req = new UpdateCompanyRequest();
        req.name = "Renamed Co";

        var result = companyService.update(COMPANY_ID, req);

        assertEquals("Renamed Co", result.name);
    }

    @Test
    void update_nullName_keepsOriginalName() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        var req = new UpdateCompanyRequest(); // name is null

        var result = companyService.update(COMPANY_ID, req);

        assertEquals("Acme Corp", result.name);
    }

    @Test
    void update_companyNotFound_throwsNotFound() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> companyService.update(COMPANY_ID, new UpdateCompanyRequest()));
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_found_setsDeletedAt() {
        var entity = company();
        assertNull(entity.deletedAt);
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(entity));

        var before = Instant.now();
        companyService.delete(COMPANY_ID);

        assertNotNull(entity.deletedAt);
        assertFalse(entity.deletedAt.isBefore(before));
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> companyService.delete(COMPANY_ID));
    }
}
