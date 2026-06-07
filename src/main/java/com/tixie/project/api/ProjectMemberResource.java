package com.tixie.project.api;

import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
import com.tixie.project.access.ProjectMemberEntity;
import com.tixie.project.access.ProjectMemberService;
import com.tixie.project.api.dto.access.CreateProjectMemberRequest;
import com.tixie.project.api.dto.access.ProjectMemberResponse;
import com.tixie.project.api.dto.access.UpdateProjectMemberRequest;
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

@Path("/projects/{projectId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Project Members")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class ProjectMemberResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    ProjectMemberService projectMemberService;

    @GET
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "List project members")
    @APIResponse(responseCode = "200", description = "List of project members",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = ProjectMemberResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project not found")
    public List<ProjectMemberResponse> list(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId) {
        return projectMemberService.list(projectId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Add a member to a project")
    @APIResponse(responseCode = "201", description = "Project member created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project or subject not found")
    public Response create(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Valid CreateProjectMemberRequest req) {
        var created = projectMemberService.create(projectId, req.subjectType, req.subjectId, req.role, currentUser.require());
        return Response.status(Response.Status.CREATED).entity(toResponse(created)).build();
    }

    @PATCH
    @Path("/{memberId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Update a project member role")
    @APIResponse(responseCode = "200", description = "Project member updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProjectMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project member not found")
    public ProjectMemberResponse update(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                        @Parameter(description = "Project member ID") @PathParam("memberId") UUID memberId,
                                        @Valid UpdateProjectMemberRequest req) {
        return toResponse(projectMemberService.update(projectId, memberId, req.role));
    }

    @DELETE
    @Path("/{memberId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    @Operation(summary = "Remove a member from a project")
    @APIResponse(responseCode = "204", description = "Project member removed")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Project member not found")
    public Response delete(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Parameter(description = "Project member ID") @PathParam("memberId") UUID memberId) {
        projectMemberService.delete(projectId, memberId);
        return Response.noContent().build();
    }

    private ProjectMemberResponse toResponse(ProjectMemberEntity entity) {
        var response = new ProjectMemberResponse();
        response.id = entity.id;
        response.projectId = entity.projectId;
        response.subjectType = entity.subjectType;
        response.subjectId = entity.subjectId;
        response.role = entity.role;
        response.createdAt = entity.createdAt;
        return response;
    }
}
