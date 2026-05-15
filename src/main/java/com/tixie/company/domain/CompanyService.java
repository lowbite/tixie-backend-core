package com.tixie.company.domain;

import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import com.tixie.company.api.dto.CreateCompanyRequest;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CompanyService {

    @Inject
    CompanyRepository companyRepository;

    @Transactional
    public CompanyEntity create(CreateCompanyRequest req) {
        var company = new CompanyEntity();
        company.name = req.name;
        company.createdAt = Instant.now();

        companyRepository.persist(company);
        return company;
    }

    public List<CompanyEntity> list() {
        return companyRepository.list("deletedAt is null");
    }

    public CompanyEntity getById(UUID id) {
        return companyRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Company '" + id + "' not found"));
    }

    @Transactional
    public CompanyEntity update(UUID id, UpdateCompanyRequest req) {
        var company = getById(id);

        if (req.name != null) {
            company.name = req.name;
        }

        return company;
    }

    @Transactional
    public void delete(UUID id) {
        var company = getById(id);
        company.deletedAt = Instant.now();
    }
}
