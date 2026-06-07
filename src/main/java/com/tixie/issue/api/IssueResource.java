package com.tixie.issue.api;

import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class IssueResource {

    @Inject
    IssueService issueService;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @POST
    @RequiresPermission(value = Permission.ISSUE_CREATE, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Create an issue")
    @APIResponse(responseCode = "201", description = "Issue created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = IssueResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project or referenced entity not found")
    public Response create(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Valid CreateIssueRequest req) {
        var issue = issueService.create(projectId, req);
        return Response.status(Response.Status.CREATED).entity(toResponse(issue)).build();
    }

    @GET
    @RequiresPermission(value = Permission.ISSUE_READ, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "List issues for a project")
    @APIResponse(responseCode = "200", description = "List of issues",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = IssueResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public List<IssueResponse> list(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                    @Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") int page,
                                    @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("100") int size) {
        return mapIssues(issueService.list(projectId, page, size));
    }

    public List<IssueResponse> list(UUID projectId) {
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
    @RequiresPermission(value = Permission.ISSUE_READ, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Get an issue by ID")
    @APIResponse(responseCode = "200", description = "Issue found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = IssueResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse getById(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                 @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId) {
        return toResponse(issueService.getById(projectId, issueId));
    }

    @PATCH
    @Path("/{issueId}")
    @RequiresPermission(value = Permission.ISSUE_UPDATE, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Partially update an issue")
    @APIResponse(responseCode = "200", description = "Issue updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = IssueResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse patch(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                               @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId,
                               @Valid PatchIssueRequest req) {
        return toResponse(issueService.patch(projectId, issueId, req));
    }

    @DELETE
    @Path("/{issueId}")
    @RequiresPermission(value = Permission.ISSUE_DELETE, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Soft-delete an issue and its descendants")
    @APIResponse(responseCode = "204", description = "Issue deleted")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public Response delete(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId) {
        issueService.delete(projectId, issueId);
        return Response.noContent().build();
    }

    @GET
    @Path("/board")
    @RequiresPermission(value = Permission.ISSUE_READ, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Get project board with columns and ordered issues")
    @APIResponse(responseCode = "200", description = "Project board",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectBoardResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public ProjectBoardResponse board(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId) {
        return issueService.board(projectId);
    }

    @POST
    @Path("/{issueId}/transitions")
    @RequiresPermission(value = Permission.ISSUE_TRANSITION, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Transition issue to another status")
    @APIResponse(responseCode = "200", description = "Issue transitioned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = IssueResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse transition(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                    @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId,
                                    @Valid TransitionIssueRequest req) {
        return toResponse(issueService.transition(projectId, issueId, req.targetStatusId));
    }

    @POST
    @Path("/{issueId}/move")
    @RequiresPermission(value = Permission.ISSUE_TRANSITION, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Move issue to status/position on board")
    @APIResponse(responseCode = "200", description = "Issue moved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = IssueResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse move(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                              @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId,
                              @Valid MoveIssueRequest req) {
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
