package com.tixie.auth.api;

import com.tixie.auth.api.dto.CreateCompanyOnboardingRequest;
import com.tixie.auth.api.dto.OnboardingResponse;
import com.tixie.auth.domain.IdentityService;
import com.tixie.auth.domain.OnboardingService;
import com.tixie.company.CompanyEntity;
import com.tixie.company.api.dto.CompanyResponse;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Onboarding")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class OnboardingResource {

    @Inject
    IdentityService identityService;

    @Inject
    OnboardingService onboardingService;

    @POST
    @Path("/company")
    @Operation(summary = "Create a company for the authenticated Keycloak user")
    @APIResponse(responseCode = "200", description = "Company and user created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OnboardingResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "409", description = "User is already onboarded")
    public OnboardingResponse createCompany(@Valid CreateCompanyOnboardingRequest req) {
        var result = onboardingService.createCompany(identityService.currentIdentity(), req);

        var response = new OnboardingResponse();
        response.company = toCompanyResponse(result.company());
        response.user = AuthResource.toResponse(result.user());
        return response;
    }

    private CompanyResponse toCompanyResponse(CompanyEntity company) {
        var response = new CompanyResponse();
        response.id = company.id;
        response.name = company.name;
        response.createdAt = company.createdAt;
        return response;
    }
}
