package com.tixie.auth.api;

import com.tixie.auth.CompanyInviteEntity;
import com.tixie.auth.api.dto.CompanyInviteResponse;
import com.tixie.auth.api.dto.CreateCompanyInviteRequest;
import com.tixie.auth.api.dto.CurrentUserResponse;
import com.tixie.auth.domain.CompanyInviteService;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.auth.domain.IdentityService;
import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
import com.tixie.company.CompanyEntity;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Invites")
@RunOnVirtualThread
public class InviteResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    IdentityService identityService;

    @Inject
    CompanyInviteService inviteService;

    @POST
    @Path("/companies/{companyId}/invites")
    @Authenticated
    @RequiresPermission(value = Permission.COMPANY_MANAGE_USERS, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Invite a user to the current company")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "201", description = "Invite created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CompanyInviteResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public Response create(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                           @Valid CreateCompanyInviteRequest req) {
        var user = currentUser.require();

        var created = inviteService.create(user, req.email, req.role);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(created.invite(), created.company(), created.token()))
                .build();
    }

    @GET
    @Path("/invites/{token}")
    @Operation(summary = "Get pending invite details")
    @APIResponse(responseCode = "200", description = "Invite details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CompanyInviteResponse.class)))
    @APIResponse(responseCode = "404", description = "Invite not found or expired")
    public CompanyInviteResponse get(@Parameter(description = "Invite token") @PathParam("token") String token) {
        var details = inviteService.get(token);
        return toResponse(details.invite(), details.company(), null);
    }

    @POST
    @Path("/invites/{token}/accept")
    @Authenticated
    @Operation(summary = "Accept a pending invite")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "200", description = "Invite accepted",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CurrentUserResponse.class)))
    @APIResponse(responseCode = "400", description = "Invite email does not match authenticated identity")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "404", description = "Invite not found or expired")
    public CurrentUserResponse accept(@Parameter(description = "Invite token") @PathParam("token") String token) {
        var user = inviteService.accept(token, identityService.currentIdentity());
        return AuthResource.toResponse(user);
    }

    private CompanyInviteResponse toResponse(CompanyInviteEntity invite, CompanyEntity company, String token) {
        var response = new CompanyInviteResponse();
        response.id = invite.id;
        response.companyId = invite.companyId;
        response.companyName = company.name;
        response.email = invite.email;
        response.role = invite.role;
        response.expiresAt = invite.expiresAt;
        response.token = token;
        return response;
    }
}
