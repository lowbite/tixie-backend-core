package com.tixie.company;

import com.tixie.company.api.dto.CompanyResponse;
import com.tixie.company.api.dto.CreateCompanyRequest;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyModelSmokeTest {

    @Test
    void entityAndDtos_areUsable() {
        var entity = new CompanyEntity();
        entity.id = UUID.randomUUID();
        entity.name = "Acme";
        entity.createdAt = Instant.now();

        var create = new CreateCompanyRequest();
        create.name = "Acme";
        var update = new UpdateCompanyRequest();
        update.name = "Acme 2";

        var response = new CompanyResponse();
        response.id = entity.id;
        response.name = entity.name;
        response.createdAt = entity.createdAt;
        assertEquals("Acme", response.name);
    }
}
