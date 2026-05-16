package com.tixie.auth.api;

import com.tixie.auth.UserEntity;
import com.tixie.auth.api.dto.CurrentUserResponse;
import com.tixie.auth.domain.CurrentUser;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auth")
@RunOnVirtualThread
@Authenticated
public class AuthResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/me")
    @Operation(summary = "Get current onboarded user")
    public CurrentUserResponse me() {
        return toResponse(currentUser.require());
    }

    static CurrentUserResponse toResponse(UserEntity user) {
        var response = new CurrentUserResponse();
        response.id = user.id;
        response.companyId = user.companyId;
        response.email = user.email;
        response.displayName = user.displayName;
        response.role = user.role;
        return response;
    }
}
