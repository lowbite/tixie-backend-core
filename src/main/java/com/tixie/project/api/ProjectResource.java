package com.tixie.project.api;

import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.AuthorizationService;
import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectStatusEntity;
import com.tixie.project.api.dto.CreateProjectRequest;
import com.tixie.project.api.dto.ProjectResponse;
import com.tixie.project.api.dto.UpdateProjectRequest;
import com.tixie.project.domain.ProjectService;
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

@Path("/companies/{companyId}/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class ProjectResource {

    @Inject
    ProjectService projectService;

    @Inject
    CurrentUser currentUser;

    @Inject
    AuthorizationService authorizationService;

    @POST
    @RequiresPermission(value = Permission.PROJECT_CREATE, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Create a project")
    @APIResponse(responseCode = "201", description = "Project created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public Response create(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                           @Valid CreateProjectRequest req) {
        var project = projectService.create(companyId, req);
        return Response.status(Response.Status.CREATED).entity(toResponse(project)).build();
    }

    @GET
    @RequiresPermission(value = Permission.PROJECT_READ, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "List projects for a company")
    @APIResponse(responseCode = "200", description = "List of projects",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = ProjectResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public List<ProjectResponse> list(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                                      @Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") int page,
                                      @Parameter(description = "Page size, capped at 500") @QueryParam("size") @DefaultValue("100") int size) {
        var user = currentUser.require();
        var projects = projectService.list(companyId).stream()
                .filter(project -> authorizationService.can(user, Permission.PROJECT_READ, ResourceType.PROJECT, project.id))
                .skip((long) normalizePage(page) * normalizeSize(size))
                .limit(normalizeSize(size))
                .toList();
        var statusesByProjectId = projectService.getStatusesByProjectIds(projects.stream().map(p -> p.id).toList());
        return projects.stream().map(project -> toResponse(project, statusesByProjectId)).toList();
    }

    public List<ProjectResponse> list(UUID companyId) {
        var user = currentUser.require();
        return projectService.list(companyId).stream()
                .filter(project -> authorizationService.can(user, Permission.PROJECT_READ, ResourceType.PROJECT, project.id))
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{projectId}")
    @RequiresPermission(value = Permission.PROJECT_READ, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Get a project by ID")
    @APIResponse(responseCode = "200", description = "Project found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public ProjectResponse getById(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                                   @Parameter(description = "Project ID") @PathParam("projectId") UUID projectId) {
        return toResponse(projectService.getById(companyId, projectId));
    }

    @PATCH
    @Path("/{projectId}")
    @RequiresPermission(value = Permission.PROJECT_UPDATE, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Partially update a project")
    @APIResponse(responseCode = "200", description = "Project updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public ProjectResponse update(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                                  @Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                  @Valid UpdateProjectRequest req) {
        return toResponse(projectService.update(companyId, projectId, req));
    }

    @DELETE
    @Path("/{projectId}")
    @RequiresPermission(value = Permission.PROJECT_DELETE, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Soft-delete a project")
    @APIResponse(responseCode = "204", description = "Project deleted")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public Response delete(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                           @Parameter(description = "Project ID") @PathParam("projectId") UUID projectId) {
        projectService.delete(companyId, projectId);
        return Response.noContent().build();
    }

    private ProjectResponse toResponse(ProjectEntity project) {
        return toResponse(project, Map.of(project.id, projectService.getStatuses(project.id)));
    }

    private ProjectResponse toResponse(ProjectEntity project, Map<UUID, List<ProjectStatusEntity>> statusesByProjectId) {
        var response = new ProjectResponse();
        response.id = project.id;
        response.companyId = project.companyId;
        response.name = project.name;
        response.key = project.key;
        response.accessMode = project.accessMode;
        response.createdAt = project.createdAt;
        response.statuses = statusesByProjectId.getOrDefault(project.id, List.of()).stream()
                .sorted(java.util.Comparator.comparingInt(s -> s.displayOrder))
                .map(this::toStatusRef)
                .toList();
        return response;
    }

    private ProjectResponse.StatusRef toStatusRef(ProjectStatusEntity status) {
        return new ProjectResponse.StatusRef(status.id, status.name, status.displayOrder, status.isDefault);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 500));
    }
}
