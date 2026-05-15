package com.tixie.project.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import com.tixie.project.ProjectRepository;
import com.tixie.project.api.dto.CreateProjectRequest;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectValidatorTest {

    static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock CompanyRepository companyRepository;
    @Mock ProjectRepository projectRepository;

    @InjectMocks ProjectValidator validator;

    private CompanyEntity company() {
        var c = new CompanyEntity();
        c.id = COMPANY_ID;
        c.name = "Acme Corp";
        c.createdAt = Instant.now();
        return c;
    }

    private CreateProjectRequest createReq(String key) {
        var req = new CreateProjectRequest();
        req.name = "My Project";
        req.key = key;
        return req;
    }

    // =========================================================
    // validateCreate — happy path
    // =========================================================

    @Test
    void validateCreate_companyExistsAndKeyUnique_passes() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(projectRepository.existsByKey("PROJ")).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateCreate(createReq("PROJ"), COMPANY_ID));
    }

    // =========================================================
    // validateCreate — failure paths
    // =========================================================

    @Test
    void validateCreate_companyNotFound_throwsNotFound_andKeyCheckSkipped() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> validator.validateCreate(createReq("PROJ"), COMPANY_ID));
        verifyNoInteractions(projectRepository);
    }

    @Test
    void validateCreate_duplicateKey_throwsValidationWithDuplicateKeyCode() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(projectRepository.existsByKey("PROJ")).thenReturn(true);

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq("PROJ"), COMPANY_ID));
        assertEquals("DUPLICATE_KEY", ex.getCode());
    }

    @Test
    void validateCreate_duplicateKey_messageContainsKey() {
        when(companyRepository.findActiveById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(projectRepository.existsByKey("ABC")).thenReturn(true);

        var ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(createReq("ABC"), COMPANY_ID));
        assertTrue(ex.getMessage().contains("ABC"));
    }
}
