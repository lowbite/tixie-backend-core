package com.tixie.issue.domain;

import com.tixie.issue.IssueEntity;
import com.tixie.issue.IssueRepository;
import com.tixie.issue.api.dto.ProjectBoardResponse;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.MoveIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.project.ProjectRepository;
import com.tixie.project.ProjectStatusRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class IssueService {

    @Inject
    IssueRepository issueRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    ProjectStatusRepository projectStatusRepository;

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
        issueRepository.lockProjectStatusLane(projectId, req.statusId);
        issue.position = issueRepository.nextPosition(projectId, req.statusId);
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

    public List<IssueEntity> list(UUID projectId, int page, int size) {
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
        return issueRepository.findActiveByProjectId(projectId, normalizePage(page), normalizeSize(size));
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

        if (req.isTitleSet()) issue.title = req.getTitle();
        if (req.isDescriptionSet()) issue.description = req.getDescription();
        if (req.isPrioritySet()) issue.priority = req.getPriority();
        if (req.isStatusIdSet()) issue.statusId = req.getStatusId();
        if (req.isParentIdSet()) issue.parentId = req.getParentId();
        issue.updatedAt = Instant.now();

        return issue;
    }

    @Transactional
    public void delete(UUID projectId, UUID issueId) {
        getById(projectId, issueId);
        softDeleteHandler.softDelete(issueId);
    }

    @Transactional
    public IssueEntity transition(UUID projectId, UUID issueId, UUID targetStatusId) {
        var issue = getById(projectId, issueId);
        validator.validateTransition(targetStatusId, issue);
        issueRepository.lockProjectStatusLane(projectId, targetStatusId);
        issue.statusId = targetStatusId;
        issue.position = issueRepository.nextPosition(projectId, targetStatusId);
        issue.updatedAt = Instant.now();
        return issue;
    }

    @Transactional
    public IssueEntity move(UUID projectId, UUID issueId, MoveIssueRequest req) {
        var issue = getById(projectId, issueId);
        validator.validateStatusBelongsToProject(req.targetStatusId, issue.projectId);
        lockLanes(projectId, issue.statusId, req.targetStatusId);

        boolean sameStatusMove = issue.statusId.equals(req.targetStatusId);
        var targetIssues = new ArrayList<>(issueRepository.findActiveByProjectIdAndStatusId(projectId, req.targetStatusId));
        targetIssues.removeIf(i -> i.id.equals(issue.id));

        int insertionIndex = resolveInsertionIndex(req.targetPosition, targetIssues.size());
        targetIssues.add(insertionIndex, issue);

        if (!sameStatusMove) {
            var sourceIssues = new ArrayList<>(issueRepository.findActiveByProjectIdAndStatusId(projectId, issue.statusId));
            sourceIssues.removeIf(i -> i.id.equals(issue.id));
            reindex(sourceIssues);
        }

        issue.statusId = req.targetStatusId;
        issue.updatedAt = Instant.now();
        reindex(targetIssues);
        return issue;
    }

    public ProjectBoardResponse board(UUID projectId) {
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));

        var statuses = projectStatusRepository.findActiveByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(s -> s.displayOrder))
                .toList();
        var issues = issueRepository.findActiveByProjectId(projectId);
        Map<UUID, List<IssueEntity>> issuesByStatus = issues.stream()
                .collect(Collectors.groupingBy(i -> i.statusId));

        var response = new ProjectBoardResponse();
        response.projectId = projectId;
        response.columns = statuses.stream().map(status -> {
            var cards = issuesByStatus.getOrDefault(status.id, List.of()).stream()
                    .sorted(Comparator.comparingLong(i -> i.position))
                    .map(i -> new ProjectBoardResponse.Card(
                            i.id, i.issueKey, i.title, i.description, i.type, i.priority, i.parentId, i.position
                    ))
                    .toList();
            return new ProjectBoardResponse.Column(status.id, status.name, status.displayOrder, status.isDefault, cards);
        }).toList();
        return response;
    }

    private int resolveInsertionIndex(Integer targetPosition, int currentSize) {
        if (targetPosition == null) {
            return currentSize;
        }
        if (targetPosition < 1) {
            return 0;
        }
        return Math.min(targetPosition - 1, currentSize);
    }

    private void reindex(List<IssueEntity> issues) {
        for (int i = 0; i < issues.size(); i++) {
            issues.get(i).position = i + 1L;
        }
    }

    private void lockLanes(UUID projectId, UUID firstStatusId, UUID secondStatusId) {
        if (firstStatusId.equals(secondStatusId)) {
            issueRepository.lockProjectStatusLane(projectId, firstStatusId);
            return;
        }

        var ordered = List.of(firstStatusId, secondStatusId).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        ordered.forEach(statusId -> issueRepository.lockProjectStatusLane(projectId, statusId));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 500));
    }
}
