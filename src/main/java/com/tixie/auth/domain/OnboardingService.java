package com.tixie.auth.domain;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.auth.api.dto.CreateCompanyOnboardingRequest;
import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class OnboardingService {

    @Inject
    CompanyRepository companyRepository;

    @Inject
    UserService userService;

    @Transactional
    public OnboardingResult createCompany(KeycloakIdentity identity, CreateCompanyOnboardingRequest req) {
        var company = new CompanyEntity();
        company.name = req.companyName;
        company.createdAt = Instant.now();
        companyRepository.persist(company);

        UserEntity user = userService.create(identity, company.id, UserRole.OWNER);
        return new OnboardingResult(company, user);
    }

    public record OnboardingResult(CompanyEntity company, UserEntity user) {
    }
}
