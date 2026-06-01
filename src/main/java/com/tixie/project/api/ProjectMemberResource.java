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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/projects/{projectId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Project Members")
@RunOnVirtualThread
@Authenticated
public class ProjectMemberResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    ProjectMemberService projectMemberService;

    @GET
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    public List<ProjectMemberResponse> list(@PathParam("projectId") UUID projectId) {
        return projectMemberService.list(projectId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    public Response create(@PathParam("projectId") UUID projectId, @Valid CreateProjectMemberRequest req) {
        var created = projectMemberService.create(projectId, req.subjectType, req.subjectId, req.role, currentUser.require());
        return Response.status(Response.Status.CREATED).entity(toResponse(created)).build();
    }

    @PATCH
    @Path("/{memberId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    public ProjectMemberResponse update(@PathParam("projectId") UUID projectId,
                                        @PathParam("memberId") UUID memberId,
                                        @Valid UpdateProjectMemberRequest req) {
        return toResponse(projectMemberService.update(projectId, memberId, req.role));
    }

    @DELETE
    @Path("/{memberId}")
    @RequiresPermission(value = Permission.PROJECT_MANAGE_MEMBERS, resource = ResourceType.PROJECT, idParam = "projectId")
    public Response delete(@PathParam("projectId") UUID projectId,
                           @PathParam("memberId") UUID memberId) {
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
