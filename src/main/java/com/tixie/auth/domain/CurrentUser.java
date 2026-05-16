package com.tixie.auth.domain;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRepository;
import com.tixie.auth.UserRole;
import com.tixie.project.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CurrentUser {

    @Inject
    IdentityService identityService;

    @Inject
    UserRepository userRepository;

    @Inject
    ProjectRepository projectRepository;

    public UserEntity require() {
        var identity = identityService.currentIdentity();
        return userRepository.findActiveByKeycloakSubject(identity.subject())
                .orElseThrow(() -> new NotFoundException("Authenticated user is not onboarded"));
    }

    public UserEntity requireCompany(UUID companyId) {
        var user = require();
        if (!user.companyId.equals(companyId)) {
            throw new NotFoundException("Company '" + companyId + "' not found");
        }
        return user;
    }

    public UserEntity requireProject(UUID projectId) {
        var user = require();
        var project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
        if (!project.companyId.equals(user.companyId)) {
            throw new NotFoundException("Project '" + projectId + "' not found");
        }
        return user;
    }

    public void requireAnyRole(UserEntity user, UserRole... roles) {
        if (!Set.of(roles).contains(user.role)) {
            throw new ForbiddenException("Insufficient role");
        }
    }
}
