package com.tixie.project.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.company.CompanyRepository;
import com.tixie.project.ProjectRepository;
import com.tixie.project.api.dto.CreateProjectRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;

@ApplicationScoped
public class ProjectValidator {

    @Inject
    CompanyRepository companyRepository;

    @Inject
    ProjectRepository projectRepository;

    public void validateCreate(CreateProjectRequest req, UUID companyId) {
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + companyId + "' not found"));

        if (projectRepository.existsByKey(req.key)) {
            throw new ValidationException("DUPLICATE_KEY",
                    "Project key '" + req.key + "' is already in use");
        }
    }
}
