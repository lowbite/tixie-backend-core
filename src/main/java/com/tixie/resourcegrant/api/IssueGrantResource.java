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

@Path("/projects/{projectId}/issues/{issueId}/grants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Issue Grants")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "List issue access grants")
    @APIResponse(responseCode = "200", description = "List of issue grants",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = ResourceGrantResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue not found")
    public List<ResourceGrantResponse> list(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                                            @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId) {
        issueService.getById(projectId, issueId);
        return resourceGrantService.list(ResourceType.ISSUE, issueId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.ISSUE_MANAGE_GRANTS, resource = ResourceType.ISSUE, idParam = "issueId")
    @Operation(summary = "Create an issue access grant")
    @APIResponse(responseCode = "201", description = "Issue grant created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ResourceGrantResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue or subject not found")
    public Response create(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId,
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
    @Operation(summary = "Revoke an issue access grant")
    @APIResponse(responseCode = "204", description = "Issue grant revoked")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Issue grant not found")
    public Response delete(@Parameter(description = "Project ID") @PathParam("projectId") UUID projectId,
                           @Parameter(description = "Issue ID") @PathParam("issueId") UUID issueId,
                           @Parameter(description = "Grant ID") @PathParam("grantId") UUID grantId) {
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
