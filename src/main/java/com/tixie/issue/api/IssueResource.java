package com.tixie.issue.api;

import com.tixie.issue.IssueEntity;
import com.tixie.issue.api.dto.CreateIssueRequest;
import com.tixie.issue.api.dto.IssueResponse;
import com.tixie.issue.api.dto.PatchIssueRequest;
import com.tixie.issue.domain.IssueService;
import com.tixie.project.ProjectStatusRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/projects/{projectId}/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Issues")
public class IssueResource {

    @Inject
    IssueService issueService;

    @Inject
    ProjectStatusRepository projectStatusRepository;

    @POST
    @Operation(summary = "Create an issue")
    @APIResponse(responseCode = "201", description = "Issue created")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Project or referenced entity not found")
    public Response create(@PathParam("projectId") UUID projectId, @Valid CreateIssueRequest req) {
        var issue = issueService.create(projectId, req);
        return Response.status(Response.Status.CREATED).entity(toResponse(issue)).build();
    }

    @GET
    @Operation(summary = "List issues for a project")
    @APIResponse(responseCode = "200", description = "List of issues")
    @APIResponse(responseCode = "404", description = "Project not found")
    public List<IssueResponse> list(@PathParam("projectId") UUID projectId) {
        return issueService.list(projectId).stream().map(this::toResponse).toList();
    }

    @GET
    @Path("/{issueId}")
    @Operation(summary = "Get an issue by ID")
    @APIResponse(responseCode = "200", description = "Issue found")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public IssueResponse getById(@PathParam("projectId") UUID projectId,
                                 @PathParam("issueId") UUID issueId) {
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
        return toResponse(issueService.patch(projectId, issueId, req));
    }

    @DELETE
    @Path("/{issueId}")
    @Operation(summary = "Soft-delete an issue and its descendants")
    @APIResponse(responseCode = "204", description = "Issue deleted")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public Response delete(@PathParam("projectId") UUID projectId,
                           @PathParam("issueId") UUID issueId) {
        issueService.delete(projectId, issueId);
        return Response.noContent().build();
    }

    private IssueResponse toResponse(IssueEntity issue) {
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

        projectStatusRepository.findByIdOptional(issue.statusId).ifPresent(status ->
                response.status = new IssueResponse.StatusRef(status.id, status.name));

        return response;
    }
}
