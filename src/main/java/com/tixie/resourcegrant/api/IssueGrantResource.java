package com.tixie.resourcegrant.api;

import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
import com.tixie.issue.domain.IssueService;
import com.tixie.resourcegrant.ResourceGrantEntity;
import com.tixie.resourcegrant.api.dto.CreateResourceGrantRequest;
import com.tixie.resourcegrant.api.dto.ResourceGrantResponse;
import com.tixie.resourcegrant.domain.ResourceGrantService;
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

@Path("/projects/{projectId}/issues/{issueId}/grants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Issue Grants")
@RunOnVirtualThread
@Authenticated
public class IssueGrantResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    ResourceGrantService resourceGrantService;

    @Inject
    IssueService issueService;

    @GET
    @RequiresPermission(value = Permission.ISSUE_MANAGE_GRANTS, resource = ResourceType.ISSUE, idParam = "issueId")
    public List<ResourceGrantResponse> list(@PathParam("projectId") UUID projectId,
                                            @PathParam("issueId") UUID issueId) {
        issueService.getById(projectId, issueId);
        return resourceGrantService.list(ResourceType.ISSUE, issueId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.ISSUE_MANAGE_GRANTS, resource = ResourceType.ISSUE, idParam = "issueId")
    public Response create(@PathParam("projectId") UUID projectId,
                           @PathParam("issueId") UUID issueId,
                           @Valid CreateResourceGrantRequest req) {
        issueService.getById(projectId, issueId);
        var grant = resourceGrantService.create(
                currentUser.require(),
                ResourceType.ISSUE,
                issueId,
                req.subjectType,
                req.subjectId,
                req.permission,
                req.expiresAt
        );
        return Response.status(Response.Status.CREATED).entity(toResponse(grant)).build();
    }

    @DELETE
    @Path("/{grantId}")
    @RequiresPermission(value = Permission.ISSUE_MANAGE_GRANTS, resource = ResourceType.ISSUE, idParam = "issueId")
    public Response delete(@PathParam("projectId") UUID projectId,
                           @PathParam("issueId") UUID issueId,
                           @PathParam("grantId") UUID grantId) {
        issueService.getById(projectId, issueId);
        resourceGrantService.revoke(ResourceType.ISSUE, issueId, grantId, currentUser.require());
        return Response.noContent().build();
    }

    private ResourceGrantResponse toResponse(ResourceGrantEntity grant) {
        var response = new ResourceGrantResponse();
        response.id = grant.id;
        response.companyId = grant.companyId;
        response.resourceType = grant.resourceType;
        response.resourceId = grant.resourceId;
        response.subjectType = grant.subjectType;
        response.subjectId = grant.subjectId;
        response.permission = grant.permission;
        response.expiresAt = grant.expiresAt;
        response.createdAt = grant.createdAt;
        return response;
    }
}
