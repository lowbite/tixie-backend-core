package com.tixie.project.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.issue.IssueRepository;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectStatusService {

    @Inject
    ProjectService projectService;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @Inject
    IssueRepository issueRepository;

    public List<ProjectStatusEntity> list(UUID projectId) {
        projectService.getByIdForProjectOnly(projectId);
        return projectStatusRepository.findActiveByProjectId(projectId).stream()
                .sorted(java.util.Comparator.comparingInt(s -> s.displayOrder))
                .toList();
    }

    @Transactional
    public ProjectStatusEntity create(UUID projectId, String name, Integer displayOrder, Boolean isDefault) {
        projectService.getByIdForProjectOnly(projectId);
        validateNameUnique(projectId, null, name);

        var status = new ProjectStatusEntity();
        status.projectId = projectId;
        status.name = name;
        status.displayOrder = displayOrder != null ? displayOrder : nextDisplayOrder(projectId);
        status.isDefault = isDefault != null && isDefault;

        if (status.isDefault) {
            unsetCurrentDefault(projectId);
        }

        projectStatusRepository.persist(status);
        return status;
    }

    @Transactional
    public ProjectStatusEntity patch(UUID projectId, UUID statusId, String name, Integer displayOrder, Boolean isDefault) {
        var status = getActiveStatus(projectId, statusId);

        if (name != null && !name.equals(status.name)) {
            validateNameUnique(projectId, statusId, name);
            status.name = name;
        }

        if (displayOrder != null) {
            status.displayOrder = displayOrder;
        }

        if (isDefault != null && isDefault && !status.isDefault) {
            unsetCurrentDefault(projectId);
            status.isDefault = true;
        }

        if (isDefault != null && !isDefault && status.isDefault) {
            throw new ValidationException("INVALID_DEFAULT", "Cannot unset default without assigning another default status");
        }

        return status;
    }

    @Transactional
    public void reorder(UUID projectId, List<UUID> orderedStatusIds) {
        var statuses = projectStatusRepository.findActiveByProjectId(projectId);
        if (statuses.size() != orderedStatusIds.size()) {
            throw new ValidationException("INVALID_REORDER", "Reorder payload must include all active statuses exactly once");
        }

        var expectedIds = statuses.stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet());
        var receivedIds = new HashSet<>(orderedStatusIds);
        if (!expectedIds.equals(receivedIds) || orderedStatusIds.size() != receivedIds.size()) {
            throw new ValidationException("INVALID_REORDER", "Reorder payload must include all active statuses exactly once");
        }

        Map<UUID, ProjectStatusEntity> statusesById = statuses.stream()
                .collect(Collectors.toMap(s -> s.id, Function.identity()));
        for (int i = 0; i < orderedStatusIds.size(); i++) {
            UUID statusId = orderedStatusIds.get(i);
            int newOrder = i + 1;
            statusesById.get(statusId).displayOrder = newOrder;
        }
    }

    @Transactional
    public void delete(UUID projectId, UUID statusId, UUID moveIssuesToStatusId) {
        var status = getActiveStatus(projectId, statusId);
        var statuses = projectStatusRepository.findActiveByProjectId(projectId);

        if (statuses.size() == 1) {
            throw new ValidationException("LAST_STATUS", "Cannot delete the last active status");
        }

        if (status.isDefault) {
            throw new ValidationException("DEFAULT_STATUS", "Cannot delete default status");
        }

        long issueCount = issueRepository.countActiveByProjectIdAndStatusId(projectId, statusId);
        if (issueCount > 0 && moveIssuesToStatusId == null) {
            throw new ValidationException("STATUS_IN_USE", "Status contains issues. Provide moveIssuesTo query param.");
        }

        if (moveIssuesToStatusId != null) {
            if (moveIssuesToStatusId.equals(statusId)) {
                throw new ValidationException("INVALID_MOVE_TARGET", "Move target must differ from deleted status");
            }
            getActiveStatus(projectId, moveIssuesToStatusId);
            issueRepository.moveActiveIssuesToStatus(projectId, statusId, moveIssuesToStatusId);
        }

        status.deletedAt = Instant.now();
    }

    private ProjectStatusEntity getActiveStatus(UUID projectId, UUID statusId) {
        projectService.getByIdForProjectOnly(projectId);
        return projectStatusRepository.findActiveByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new NotFoundException("Status '" + statusId + "' not found"));
    }

    private void validateNameUnique(UUID projectId, UUID statusId, String name) {
        boolean exists = statusId == null
                ? projectStatusRepository.existsActiveByName(projectId, name)
                : projectStatusRepository.existsActiveByNameExcludingId(projectId, statusId, name);
        if (exists) {
            throw new ValidationException("DUPLICATE_STATUS_NAME",
                    "Status name '" + name + "' is already used in this project");
        }
    }

    private int nextDisplayOrder(UUID projectId) {
        return projectStatusRepository.findActiveByProjectId(projectId).stream()
                .mapToInt(s -> s.displayOrder)
                .max()
                .orElse(0) + 1;
    }

    private void unsetCurrentDefault(UUID projectId) {
        projectStatusRepository.findActiveDefaultByProjectId(projectId).ifPresent(s -> s.isDefault = false);
    }
}
