package com.tixie.issue.api;

import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.issue.IssueEntity;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.IssueResponse;
import com.tixie.issue.api.dto.MoveIssueRequest;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.api.dto.ProjectBoardResponse;
import com.tixie.issue.api.dto.TransitionIssueRequest;
import com.tixie.issue.domain.IssueService;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.ProjectStatusRepository;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/projects/{projectId}/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Issues")
@RunOnVirtualThread
@Authenticated
public class IssueResource {

    @Inject
    IssueService issueService;

    @Inject
    CurrentUser currentUser;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @POST
    @Operation(summary = "Create an issue")
    @APIResponse(responseCode = "201", description = "Issue created")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Project or referenced entity not found")
    public Response create(@PathParam("projectId") UUID projectId, @Valid CreateIssueRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN, UserRole.MEMBER);
        var issue = issueService.create(projectId, req);
        return Response.status(Response.Status.CREATED).entity(toResponse(issue)).build();
    }

    @GET
    @Operation(summary = "List issues for a project")
    @APIResponse(responseCode = "200", description = "List of issues")
    @APIResponse(responseCode = "404", description = "Project not found")
    public List<IssueResponse> list(@PathParam("projectId") UUID projectId,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("100") int size) {
        currentUser.requireProject(projectId);
        return mapIssues(issueService.list(projectId, page, size));
    }

    public List<IssueResponse> list(UUID projectId) {
        currentUser.requireProject(projectId);
        return issueService.list(projectId).stream().map(this::toResponse).toList();
    }

    private List<IssueResponse> mapIssues(List<IssueEntity> issues) {
        var statusesById = projectStatusRepository.findActiveByIds(
                        issues.stream().map(i -> i.statusId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(s -> s.id, Function.identity()));
        return issues.stream().map(issue -> toResponse(issue, statusesById)).toList();
    }

    @GET
    @Path("/{issueId}")
    @Operation(summary = "Get an issue by ID")
    @APIResponse(responseCode = "200", description = "Issue found")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse getById(@PathParam("projectId") UUID projectId,
                                 @PathParam("issueId") UUID issueId) {
        currentUser.requireProject(projectId);
        return toResponse(issueService.getById(projectId, issueId));
    }

    @PATCH
    @Path("/{issueId}")
    @Operation(summary = "Partially update an issue")
    @APIResponse(responseCode = "200", description = "Issue updated")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse patch(@PathParam("projectId") UUID projectId,
                               @PathParam("issueId") UUID issueId,
                               @Valid PatchIssueRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN, UserRole.MEMBER);
        return toResponse(issueService.patch(projectId, issueId, req));
    }

    @DELETE
    @Path("/{issueId}")
    @Operation(summary = "Soft-delete an issue and its descendants")
    @APIResponse(responseCode = "204", description = "Issue deleted")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public Response delete(@PathParam("projectId") UUID projectId,
                           @PathParam("issueId") UUID issueId) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        issueService.delete(projectId, issueId);
        return Response.noContent().build();
    }

    @GET
    @Path("/board")
    @Operation(summary = "Get project board with columns and ordered issues")
    public ProjectBoardResponse board(@PathParam("projectId") UUID projectId) {
        currentUser.requireProject(projectId);
        return issueService.board(projectId);
    }

    @POST
    @Path("/{issueId}/transitions")
    @Operation(summary = "Transition issue to another status")
    @APIResponse(responseCode = "200", description = "Issue transitioned")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse transition(@PathParam("projectId") UUID projectId,
                                    @PathParam("issueId") UUID issueId,
                                    @Valid TransitionIssueRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN, UserRole.MEMBER);
        return toResponse(issueService.transition(projectId, issueId, req.targetStatusId));
    }

    @POST
    @Path("/{issueId}/move")
    @Operation(summary = "Move issue to status/position on board")
    public IssueResponse move(@PathParam("projectId") UUID projectId,
                              @PathParam("issueId") UUID issueId,
                              @Valid MoveIssueRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN, UserRole.MEMBER);
        return toResponse(issueService.move(projectId, issueId, req));
    }

    private IssueResponse toResponse(IssueEntity issue) {
        return toResponse(issue, Map.of());
    }

    private IssueResponse toResponse(IssueEntity issue, Map<UUID, ProjectStatusEntity> statusesById) {
        var response = new IssueResponse();
        response.id = issue.id;
        response.issueKey = issue.issueKey;
        response.title = issue.title;
        response.description = issue.description;
        response.type = issue.type;
        response.priority = issue.priority;
        response.projectId = issue.projectId;
        response.parentId = issue.parentId;
        response.createdAt = issue.createdAt;
        response.updatedAt = issue.updatedAt;

        var status = statusesById.get(issue.statusId);
        if (status != null) {
            response.status = new IssueResponse.StatusRef(status.id, status.name);
        } else {
            projectStatusRepository.findByIdOptional(issue.statusId).ifPresent(found ->
                    response.status = new IssueResponse.StatusRef(found.id, found.name));
        }

        return response;
    }
}
