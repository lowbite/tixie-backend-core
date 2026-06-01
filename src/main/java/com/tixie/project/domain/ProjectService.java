package com.tixie.project.domain;

import com.tixie.company.CompanyRepository;
import com.tixie.authz.ProjectAccessMode;
import com.tixie.issue.domain.IssueSoftDeleteHandler;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import com.tixie.project.api.dto.CreateProjectRequest;
import com.tixie.project.api.dto.UpdateProjectRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectService {

    @Inject
    ProjectRepository projectRepository;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    ProjectValidator validator;

    @Inject
    IssueSoftDeleteHandler issueSoftDeleteHandler;

    @Transactional
    public ProjectEntity create(UUID companyId, CreateProjectRequest req) {
        validator.validateCreate(req, companyId);

        var project = new ProjectEntity();
        project.companyId = companyId;
        project.name = req.name;
        project.key = req.key;
        project.accessMode = req.accessMode != null ? req.accessMode : ProjectAccessMode.COMPANY;
        project.createdAt = Instant.now();

        projectRepository.persist(project);
        seedDefaultStatuses(project.id);

        return project;
    }

    public List<ProjectEntity> list(UUID companyId) {
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + companyId + "' not found"));

        return projectRepository.findActiveByCompanyId(companyId);
    }

    public List<ProjectEntity> list(UUID companyId, int page, int size) {
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + companyId + "' not found"));

        return projectRepository.findActiveByCompanyId(companyId, normalizePage(page), normalizeSize(size));
    }

    public ProjectEntity getById(UUID companyId, UUID projectId) {
        return projectRepository.findActiveById(projectId)
                .filter(p -> p.companyId.equals(companyId))
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
    }

    public ProjectEntity getByIdForProjectOnly(UUID projectId) {
        return projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
    }

    @Transactional
    public ProjectEntity update(UUID companyId, UUID projectId, UpdateProjectRequest req) {
        var project = getById(companyId, projectId);

        if (req.name != null) {
            project.name = req.name;
        }
        if (req.accessMode != null) {
            project.accessMode = req.accessMode;
        }

        return project;
    }

    @Transactional
    public void delete(UUID companyId, UUID projectId) {
        var project = getById(companyId, projectId);
        issueSoftDeleteHandler.softDeleteByProjectId(project.id);
        projectStatusRepository.softDeleteActiveByProjectId(project.id);
        project.deletedAt = Instant.now();
    }

    public List<ProjectStatusEntity> getStatuses(UUID projectId) {
        return projectStatusRepository.findActiveByProjectId(projectId);
    }

    public Map<UUID, List<ProjectStatusEntity>> getStatusesByProjectIds(List<UUID> projectIds) {
        return projectStatusRepository.findActiveByProjectIds(projectIds).stream()
                .collect(Collectors.groupingBy(s -> s.projectId));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 500));
    }

    private void seedDefaultStatuses(UUID projectId) {
        var statuses = List.of(
                buildStatus(projectId, "To Do",       1, true),
                buildStatus(projectId, "In Progress",  2, false),
                buildStatus(projectId, "Done",         3, false)
        );
        statuses.forEach(projectStatusRepository::persist);
    }

    private ProjectStatusEntity buildStatus(UUID projectId, String name, int order, boolean isDefault) {
        var status = new ProjectStatusEntity();
        status.projectId = projectId;
        status.name = name;
        status.displayOrder = order;
        status.isDefault = isDefault;
        return status;
    }
}
