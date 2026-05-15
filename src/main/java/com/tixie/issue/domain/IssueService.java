package com.tixie.issue.domain;

import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.project.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class IssueService {

    @Inject
    IssueRepository issueRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    IssueValidator validator;

    @Inject
    IssueKeyGenerator keyGenerator;

    @Inject
    IssueSoftDeleteHandler softDeleteHandler;

    @Transactional
    public IssueEntity create(UUID projectId, CreateIssueRequest req) {
        validator.validateCreate(req, projectId);

        var issue = new IssueEntity();
        issue.issueKey = keyGenerator.generate(projectId);
        issue.title = req.title;
        issue.description = req.description;
        issue.type = req.type;
        issue.priority = req.priority != null ? req.priority : IssuePriority.MEDIUM;
        issue.statusId = req.statusId;
        issue.projectId = projectId;
        issue.parentId = req.parentId;
        issue.createdAt = Instant.now();
        issue.updatedAt = Instant.now();

        issueRepository.persist(issue);
        return issue;
    }

    public List<IssueEntity> list(UUID projectId) {
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
        return issueRepository.findActiveByProjectId(projectId);
    }

    public IssueEntity getById(UUID projectId, UUID issueId) {
        return issueRepository.findActiveById(issueId)
                .filter(i -> i.projectId.equals(projectId))
                .orElseThrow(() -> new NotFoundException("Issue '" + issueId + "' not found"));
    }

    @Transactional
    public IssueEntity patch(UUID projectId, UUID issueId, PatchIssueRequest req) {
        var issue = getById(projectId, issueId);
        validator.validatePatch(req, issue);

        if (req.title != null) issue.title = req.title;
        if (req.description != null) issue.description = req.description;
        if (req.priority != null) issue.priority = req.priority;
        if (req.statusId != null) issue.statusId = req.statusId;
        if (req.parentId != null) issue.parentId = req.parentId;
        issue.updatedAt = Instant.now();

        return issue;
    }

    @Transactional
    public void delete(UUID projectId, UUID issueId) {
        getById(projectId, issueId);
        softDeleteHandler.softDelete(issueId);
    }
}
