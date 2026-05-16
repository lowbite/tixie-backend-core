package com.tixie.issue.api;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.MoveIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.api.dto.ProjectBoardResponse;
import com.tixie.issue.api.dto.TransitionIssueRequest;
import com.tixie.issue.domain.IssueService;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IssueResourceTest {

    @Test
    void endpoints_delegateAndMap() {
        var service = mock(IssueService.class);
        var statuses = mock(ProjectStatusRepository.class);
        var currentUser = mock(CurrentUser.class);
        var resource = new IssueResource();
        resource.issueService = service;
        resource.projectStatusRepository = statuses;
        resource.currentUser = currentUser;

        UUID projectId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();
        var issue = issue(projectId, issueId);
        when(currentUser.requireProject(projectId)).thenReturn(user(UserRole.ADMIN));
        when(service.create(eq(projectId), any(CreateIssueRequest.class))).thenReturn(issue);
        when(service.list(projectId)).thenReturn(List.of(issue));
        when(service.getById(projectId, issueId)).thenReturn(issue);
        when(service.patch(eq(projectId), eq(issueId), any(PatchIssueRequest.class))).thenReturn(issue);
        when(service.transition(eq(projectId), eq(issueId), any())).thenReturn(issue);
        when(service.move(eq(projectId), eq(issueId), any())).thenReturn(issue);
        when(service.board(projectId)).thenReturn(new ProjectBoardResponse());

        var status = new ProjectStatusEntity();
        status.id = issue.statusId;
        status.name = "To Do";
        when(statuses.findByIdOptional(issue.statusId)).thenReturn(java.util.Optional.of(status));

        assertEquals(201, resource.create(projectId, new CreateIssueRequest()).getStatus());
        assertEquals(1, resource.list(projectId).size());
        assertNotNull(resource.getById(projectId, issueId));
        assertNotNull(resource.patch(projectId, issueId, new PatchIssueRequest()));
        assertEquals(204, resource.delete(projectId, issueId).getStatus());
        assertNotNull(resource.board(projectId));

        var tr = new TransitionIssueRequest();
        tr.targetStatusId = issue.statusId;
        assertNotNull(resource.transition(projectId, issueId, tr));

        var mv = new MoveIssueRequest();
        mv.targetStatusId = issue.statusId;
        assertNotNull(resource.move(projectId, issueId, mv));
    }

    private IssueEntity issue(UUID projectId, UUID issueId) {
        var i = new IssueEntity();
        i.id = issueId;
        i.issueKey = "PRJ-1";
        i.title = "Issue";
        i.type = IssueType.TASK;
        i.priority = IssuePriority.MEDIUM;
        i.statusId = UUID.randomUUID();
        i.projectId = projectId;
        i.createdAt = Instant.now();
        i.updatedAt = Instant.now();
        return i;
    }

    private UserEntity user(UserRole role) {
        var user = new UserEntity();
        user.id = UUID.randomUUID();
        user.companyId = UUID.randomUUID();
        user.role = role;
        return user;
    }
}
