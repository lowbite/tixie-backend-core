package com.tixie.issue.domain;

import com.tixie.common.exception.ValidationException;
import com.tixie.company.CompanyRepository;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.model.IssueType;
import com.tixie.project.ProjectRepository;
import com.tixie.project.ProjectStatusRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;

@ApplicationScoped
public class IssueValidator {

    @Inject
    CompanyRepository companyRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @Inject
    IssueRepository issueRepository;

    public void validateCreate(CreateIssueRequest req, UUID projectId) {
        var project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));

        companyRepository.findActiveById(project.companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + project.companyId + "' not found"));

        projectStatusRepository.findActiveByIdAndProjectId(req.statusId, projectId)
                .orElseThrow(() -> new ValidationException("INVALID_STATUS",
                        "Status '" + req.statusId + "' does not belong to project '" + projectId + "'"));

        if (req.type == IssueType.EPIC && req.parentId != null) {
            throw new ValidationException("INVALID_PARENT", "EPIC issues cannot have a parent");
        }

        if (req.parentId != null) {
            validateParent(req.parentId, projectId, req.type);
        }
    }

    public void validatePatch(PatchIssueRequest req, IssueEntity existing) {
        if (req.isTypeSet()) {
            throw new ValidationException("IMMUTABLE_TYPE", "Issue type cannot be changed");
        }

        if (req.isTitleSet() && req.getTitle() == null) {
            throw new ValidationException("INVALID_TITLE", "Issue title cannot be null");
        }

        if (req.isPrioritySet() && req.getPriority() == null) {
            throw new ValidationException("INVALID_PRIORITY", "Issue priority cannot be null");
        }

        if (req.isStatusIdSet()) {
            if (req.getStatusId() == null) {
                throw new ValidationException("INVALID_STATUS", "Issue status cannot be null");
            }
            projectStatusRepository.findActiveByIdAndProjectId(req.getStatusId(), existing.projectId)
                    .orElseThrow(() -> new ValidationException("INVALID_STATUS",
                            "Status '" + req.getStatusId() + "' does not belong to project '" + existing.projectId + "'"));
        }

        if (req.isParentIdSet() && req.getParentId() != null) {
            if (existing.type == IssueType.EPIC) {
                throw new ValidationException("INVALID_PARENT", "EPIC issues cannot have a parent");
            }
            validateParent(req.getParentId(), existing.projectId, existing.type);
        }
    }

    public void validateTransition(UUID targetStatusId, IssueEntity existing) {
        if (existing.statusId.equals(targetStatusId)) {
            throw new ValidationException("NOOP_TRANSITION", "Issue is already in target status");
        }

        validateStatusBelongsToProject(targetStatusId, existing.projectId);
    }

    public void validateStatusBelongsToProject(UUID targetStatusId, UUID projectId) {
        projectStatusRepository.findActiveByIdAndProjectId(targetStatusId, projectId)
                .orElseThrow(() -> new ValidationException("INVALID_STATUS",
                        "Status '" + targetStatusId + "' does not belong to project '" + projectId + "'"));
    }

    private void validateParent(UUID parentId, UUID projectId, IssueType type) {
        var parent = issueRepository.findActiveById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent issue '" + parentId + "' not found"));

        if (!parent.projectId.equals(projectId)) {
            throw new ValidationException("INVALID_PARENT",
                    "Parent issue '" + parentId + "' belongs to a different project");
        }

        IssueType allowedParentType = type.allowedParentType()
                .orElseThrow(() -> new ValidationException("INVALID_PARENT", "EPIC issues cannot have a parent"));

        if (parent.type != allowedParentType) {
            throw new ValidationException("INVALID_PARENT",
                    "Issue of type " + type + " requires a parent of type " + allowedParentType
                            + ", but parent is of type " + parent.type);
        }
    }
}
