package com.tixie.resourcegrant.domain;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRepository;
import com.tixie.authz.*;
import com.tixie.group.GroupRepository;
import com.tixie.resourcegrant.ResourceGrantEntity;
import com.tixie.resourcegrant.ResourceGrantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ResourceGrantService {

    @Inject
    ResourceGrantRepository resourceGrantRepository;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupRepository groupRepository;

    public List<ResourceGrantEntity> list(ResourceType resourceType, UUID resourceId) {
        return resourceGrantRepository.findActiveByResource(resourceType, resourceId, Instant.now());
    }

    @Transactional
    public ResourceGrantEntity create(UserEntity grantor, ResourceType resourceType, UUID resourceId,
                                      SubjectType subjectType, UUID subjectId, Permission permission,
                                      Instant expiresAt) {
        var context = authorizationService.resolve(resourceType, resourceId, grantor);
        validateSubject(context.companyId(), subjectType, subjectId);
        if (grantor.role != com.tixie.auth.UserRole.OWNER
                && !authorizationService.can(grantor, permission, resourceType, resourceId)) {
            throw new ForbiddenException("Cannot grant permission " + permission);
        }

        var grant = new ResourceGrantEntity();
        grant.companyId = context.companyId();
        grant.resourceType = resourceType;
        grant.resourceId = resourceId;
        grant.subjectType = subjectType;
        grant.subjectId = subjectId;
        grant.permission = permission;
        grant.createdByUserId = grantor.id;
        grant.createdAt = Instant.now();
        grant.expiresAt = expiresAt;
        resourceGrantRepository.persist(grant);
        return grant;
    }

    @Transactional
    public void revoke(ResourceType resourceType, UUID resourceId, UUID grantId, UserEntity currentUser) {
        var grant = resourceGrantRepository.findByIdOptional(grantId)
                .filter(found -> found.revokedAt == null)
                .orElseThrow(() -> new NotFoundException("Resource grant '" + grantId + "' not found"));
        if (!grant.companyId.equals(currentUser.companyId)
                || grant.resourceType != resourceType
                || !grant.resourceId.equals(resourceId)) {
            throw new NotFoundException("Resource grant '" + grantId + "' not found");
        }
        grant.revokedAt = Instant.now();
    }

    private void validateSubject(UUID companyId, SubjectType subjectType, UUID subjectId) {
        if (subjectType == SubjectType.USER) {
            userRepository.findActiveByIdAndCompanyId(subjectId, companyId)
                    .orElseThrow(() -> new NotFoundException("User '" + subjectId + "' not found"));
            return;
        }

        groupRepository.findActiveById(subjectId)
                .filter(group -> group.companyId.equals(companyId))
                .orElseThrow(() -> new NotFoundException("Group '" + subjectId + "' not found"));
    }
}
