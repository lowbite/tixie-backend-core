package com.tixie.project.api;

import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/projects/{projectId}/statuses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Project Statuses")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class ProjectStatusResource {

    @Inject
    ProjectStatusService projectStatusService;

    @GET
    @RequiresPermission(value = Permission.PROJECT_READ, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "List active statuses for project")
    @APIResponse(responseCode = "200", description = "List of project statuses",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = ProjectStatusResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public List<ProjectStatusResponse> list(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId) {
        return projectStatusService.list(projectId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.PROJECT_MANAGE_STATUSES, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Create project status")
    @APIResponse(responseCode = "201", description = "Status created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectStatusResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public Response create(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Valid CreateProjectStatusRequest req) {
        var created = projectStatusService.create(projectId, req.name, req.displayOrder, req.isDefault);
        return Response.status(Response.Status.CREATED).entity(toResponse(created)).build();
    }

    @PATCH
    @Path("/{statusId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_STATUSES, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Patch project status")
    @APIResponse(responseCode = "200", description = "Status updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectStatusResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project status not found")
    public ProjectStatusResponse patch(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                       @Parameter(description = "Project status ID") @PathParam("statusId") UUID statusId,
                                       @Valid PatchProjectStatusRequest req) {
        return toResponse(projectStatusService.patch(projectId, statusId, req.name, req.displayOrder, req.isDefault));
    }

    @PATCH
    @Path("/reorder")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_STATUSES, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Reorder project statuses")
    @APIResponse(responseCode = "200", description = "Statuses reordered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = ProjectStatusResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project status not found")
    public List<ProjectStatusResponse> reorder(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                               @Valid ReorderProjectStatusesRequest req) {
        projectStatusService.reorder(projectId, req.statusIds);
        return projectStatusService.list(projectId).stream().map(this::toResponse).toList();
    }

    @DELETE
    @Path("/{statusId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_STATUSES, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Delete project status")
    @APIResponse(responseCode = "204", description = "Status deleted")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project status not found")
    public Response delete(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Parameter(description = "Project status ID") @PathParam("statusId") UUID statusId,
                           @Parameter(description = "Replacement status ID for issues assigned to the deleted status") @QueryParam("moveIssuesTo") UUID moveIssuesTo) {
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
