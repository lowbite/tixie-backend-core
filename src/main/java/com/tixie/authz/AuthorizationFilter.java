package com.tixie.authz;

import com.tixie.auth.domain.CurrentUser;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

@Provider
@RequiresPermission(value = Permission.COMPANY_READ, resource = ResourceType.COMPANY)
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Context
    UriInfo uriInfo;

    @Inject
    CurrentUser currentUser;

    @Inject
    AuthorizationService authorizationService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var annotation = findAnnotation();
        if (annotation == null) {
            return;
        }

        var user = currentUser.require();
        UUID resourceId = resolveResourceId(annotation);
        authorizationService.require(user, annotation.value(), annotation.resource(), resourceId);
    }

    private RequiresPermission findAnnotation() {
        var method = resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(RequiresPermission.class)) {
            return method.getAnnotation(RequiresPermission.class);
        }

        var resourceClass = resourceInfo.getResourceClass();
        if (resourceClass != null && resourceClass.isAnnotationPresent(RequiresPermission.class)) {
            return resourceClass.getAnnotation(RequiresPermission.class);
        }

        return null;
    }

    private UUID resolveResourceId(RequiresPermission annotation) {
        if (annotation.idParam().isBlank()) {
            return currentUser.require().companyId;
        }

        var values = uriInfo.getPathParameters().get(annotation.idParam());
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Missing path parameter '" + annotation.idParam() + "'");
        }
        return UUID.fromString(values.getFirst());
    }
}
