package com.tixie.project.api;

import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.api.dto.CreateProjectStatusRequest;
import com.tixie.project.api.dto.PatchProjectStatusRequest;
import com.tixie.project.api.dto.ProjectStatusResponse;
import com.tixie.project.api.dto.ReorderProjectStatusesRequest;
import com.tixie.project.domain.ProjectStatusService;
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
import java.util.UUID;

@Path("/projects/{projectId}/statuses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Project Statuses")
@RunOnVirtualThread
@Authenticated
public class ProjectStatusResource {

    @Inject
    ProjectStatusService projectStatusService;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "List active statuses for project")
    public List<ProjectStatusResponse> list(@PathParam("projectId") UUID projectId) {
        currentUser.requireProject(projectId);
        return projectStatusService.list(projectId).stream().map(this::toResponse).toList();
    }

    @POST
    @Operation(summary = "Create project status")
    @APIResponse(responseCode = "201", description = "Status created")
    public Response create(@PathParam("projectId") UUID projectId,
                           @Valid CreateProjectStatusRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        var created = projectStatusService.create(projectId, req.name, req.displayOrder, req.isDefault);
        return Response.status(Response.Status.CREATED).entity(toResponse(created)).build();
    }

    @PATCH
    @Path("/{statusId}")
    @Operation(summary = "Patch project status")
    public ProjectStatusResponse patch(@PathParam("projectId") UUID projectId,
                                       @PathParam("statusId") UUID statusId,
                                       @Valid PatchProjectStatusRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        return toResponse(projectStatusService.patch(projectId, statusId, req.name, req.displayOrder, req.isDefault));
    }

    @PATCH
    @Path("/reorder")
    @Operation(summary = "Reorder project statuses")
    public List<ProjectStatusResponse> reorder(@PathParam("projectId") UUID projectId,
                                               @Valid ReorderProjectStatusesRequest req) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        projectStatusService.reorder(projectId, req.statusIds);
        return projectStatusService.list(projectId).stream().map(this::toResponse).toList();
    }

    @DELETE
    @Path("/{statusId}")
    @Operation(summary = "Delete project status")
    public Response delete(@PathParam("projectId") UUID projectId,
                           @PathParam("statusId") UUID statusId,
                           @QueryParam("moveIssuesTo") UUID moveIssuesTo) {
        var user = currentUser.requireProject(projectId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        projectStatusService.delete(projectId, statusId, moveIssuesTo);
        return Response.noContent().build();
    }

    private ProjectStatusResponse toResponse(ProjectStatusEntity entity) {
        var response = new ProjectStatusResponse();
        response.id = entity.id;
        response.projectId = entity.projectId;
        response.name = entity.name;
        response.displayOrder = entity.displayOrder;
        response.isDefault = entity.isDefault;
        return response;
    }
}
