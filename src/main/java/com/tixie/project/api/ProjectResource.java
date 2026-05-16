package com.tixie.project.api;

import com.tixie.auth.UserRole;
import com.tixie.auth.domain.CurrentUser;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/companies/{companyId}/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Projects")
@RunOnVirtualThread
@Authenticated
public class ProjectResource {

    @Inject
    ProjectService projectService;

    @Inject
    CurrentUser currentUser;

    @POST
    @Operation(summary = "Create a project")
    @APIResponse(responseCode = "201", description = "Project created")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Company not found")
    public Response create(@PathParam("companyId") UUID companyId,
                           @Valid CreateProjectRequest req) {
        var user = currentUser.requireCompany(companyId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        var project = projectService.create(companyId, req);
        return Response.status(Response.Status.CREATED).entity(toResponse(project)).build();
    }

    @GET
    @Operation(summary = "List projects for a company")
    @APIResponse(responseCode = "200", description = "List of projects")
    @APIResponse(responseCode = "404", description = "Company not found")
    public List<ProjectResponse> list(@PathParam("companyId") UUID companyId,
                                      @QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("100") int size) {
        currentUser.requireCompany(companyId);
        var projects = projectService.list(companyId, page, size);
        var statusesByProjectId = projectService.getStatusesByProjectIds(projects.stream().map(p -> p.id).toList());
        return projects.stream().map(project -> toResponse(project, statusesByProjectId)).toList();
    }

    public List<ProjectResponse> list(UUID companyId) {
        currentUser.requireCompany(companyId);
        return projectService.list(companyId).stream().map(this::toResponse).toList();
    }

    @GET
    @Path("/{projectId}")
    @Operation(summary = "Get a project by ID")
    @APIResponse(responseCode = "200", description = "Project found")
    @APIResponse(responseCode = "404", description = "Project not found")
    public ProjectResponse getById(@PathParam("companyId") UUID companyId,
                                   @PathParam("projectId") UUID projectId) {
        currentUser.requireCompany(companyId);
        return toResponse(projectService.getById(companyId, projectId));
    }

    @PATCH
    @Path("/{projectId}")
    @Operation(summary = "Partially update a project")
    @APIResponse(responseCode = "200", description = "Project updated")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Project not found")
    public ProjectResponse update(@PathParam("companyId") UUID companyId,
                                  @PathParam("projectId") UUID projectId,
                                  @Valid UpdateProjectRequest req) {
        var user = currentUser.requireCompany(companyId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
        return toResponse(projectService.update(companyId, projectId, req));
    }

    @DELETE
    @Path("/{projectId}")
    @Operation(summary = "Soft-delete a project")
    @APIResponse(responseCode = "204", description = "Project deleted")
    @APIResponse(responseCode = "404", description = "Project not found")
    public Response delete(@PathParam("companyId") UUID companyId,
                           @PathParam("projectId") UUID projectId) {
        var user = currentUser.requireCompany(companyId);
        currentUser.requireAnyRole(user, UserRole.OWNER, UserRole.ADMIN);
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
}
