package com.tixie.company.api;

import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.Permission;
import com.tixie.authz.RequiresPermission;
import com.tixie.authz.ResourceType;
import com.tixie.company.CompanyEntity;
import com.tixie.company.api.dto.CompanyResponse;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import com.tixie.company.domain.CompanyService;
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

@Path("/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Companies")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class CompanyResource {

    @Inject
    CompanyService companyService;

    @Inject
    CurrentUser currentUser;

    @GET
    @RequiresPermission(value = Permission.COMPANY_READ, resource = ResourceType.COMPANY)
    @Operation(summary = "List companies visible to the current user")
    @APIResponse(responseCode = "200", description = "List of companies",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = CompanyResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    public List<CompanyResponse> list(@Parameter(description = "Zero-based page index") @QueryParam("page") @DefaultValue("0") int page,
                                      @Parameter(description = "Page size, default 100") @QueryParam("size") @DefaultValue("100") int size) {
        var user = currentUser.require();
        return List.of(toResponse(companyService.getById(user.companyId)));
    }

    public List<CompanyResponse> list() {
        var user = currentUser.require();
        return List.of(toResponse(companyService.getById(user.companyId)));
    }

    @GET
    @Path("/{companyId}")
    @RequiresPermission(value = Permission.COMPANY_READ, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Get a company by ID")
    @APIResponse(responseCode = "200", description = "Company found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CompanyResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse getById(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId) {
        return toResponse(companyService.getById(companyId));
    }

    @PATCH
    @Path("/{companyId}")
    @RequiresPermission(value = Permission.COMPANY_UPDATE, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Partially update a company")
    @APIResponse(responseCode = "200", description = "Company updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CompanyResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse update(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId,
                                  @Valid UpdateCompanyRequest req) {
        return toResponse(companyService.update(companyId, req));
    }

    @DELETE
    @Path("/{companyId}")
    @RequiresPermission(value = Permission.COMPANY_DELETE, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Soft-delete a company")
    @APIResponse(responseCode = "204", description = "Company deleted")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Company not found")
    public Response delete(@Parameter(description = "Company ID") @PathParam("companyId") UUID companyId) {
        companyService.delete(companyId);
        return Response.noContent().build();
    }

    private CompanyResponse toResponse(CompanyEntity company) {
        var response = new CompanyResponse();
        response.id = company.id;
        response.name = company.name;
        response.createdAt = company.createdAt;
        return response;
    }
}
