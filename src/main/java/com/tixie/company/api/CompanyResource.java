package com.tixie.company.api;

import com.tixie.company.CompanyEntity;
import com.tixie.company.api.dto.CompanyResponse;
import com.tixie.company.api.dto.CreateCompanyRequest;
import com.tixie.company.api.dto.UpdateCompanyRequest;
import com.tixie.company.domain.CompanyService;
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

@Path("/api/v1/companies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Companies")
@RunOnVirtualThread
public class CompanyResource {

    @Inject
    CompanyService companyService;

    @POST
    @Operation(summary = "Create a company")
    @APIResponse(responseCode = "201", description = "Company created")
    @APIResponse(responseCode = "400", description = "Validation error")
    public Response create(@Valid CreateCompanyRequest req) {
        var company = companyService.create(req);
        return Response.status(Response.Status.CREATED).entity(toResponse(company)).build();
    }

    @GET
    @Operation(summary = "List all active companies")
    @APIResponse(responseCode = "200", description = "List of companies")
    public List<CompanyResponse> list(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("100") int size) {
        return companyService.list(page, size).stream().map(this::toResponse).toList();
    }

    public List<CompanyResponse> list() {
        return companyService.list().stream().map(this::toResponse).toList();
    }

    @GET
    @Path("/{companyId}")
    @Operation(summary = "Get a company by ID")
    @APIResponse(responseCode = "200", description = "Company found")
    @APIResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse getById(@PathParam("companyId") UUID companyId) {
        return toResponse(companyService.getById(companyId));
    }

    @PATCH
    @Path("/{companyId}")
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
