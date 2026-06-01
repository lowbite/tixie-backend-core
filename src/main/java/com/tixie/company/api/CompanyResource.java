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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Companies")
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
    @APIResponse(responseCode = "200", description = "List of companies")
    public List<CompanyResponse> list(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("100") int size) {
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
    @APIResponse(responseCode = "200", description = "Company found")
    @APIResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse getById(@PathParam("companyId") UUID companyId) {
        return toResponse(companyService.getById(companyId));
    }

    @PATCH
    @Path("/{companyId}")
    @RequiresPermission(value = Permission.COMPANY_UPDATE, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Partially update a company")
    @APIResponse(responseCode = "200", description = "Company updated")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse update(@PathParam("companyId") UUID companyId,
                                  @Valid UpdateCompanyRequest req) {
        return toResponse(companyService.update(companyId, req));
    }

    @DELETE
    @Path("/{companyId}")
    @RequiresPermission(value = Permission.COMPANY_DELETE, resource = ResourceType.COMPANY, idParam = "companyId")
    @Operation(summary = "Soft-delete a company")
    @APIResponse(responseCode = "204", description = "Company deleted")
    @APIResponse(responseCode = "404", description = "Company not found")
    public Response delete(@PathParam("companyId") UUID companyId) {
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
