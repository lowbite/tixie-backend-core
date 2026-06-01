package com.tixie.resourcegrant;

import com.tixie.authz.Permission;
import com.tixie.authz.ResourceType;
import com.tixie.authz.SubjectType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ResourceGrantRepository implements PanacheRepositoryBase<ResourceGrantEntity, UUID> {

    public List<ResourceGrantEntity> findActiveByResource(ResourceType resourceType, UUID resourceId, Instant now) {
        return list("""
                resourceType = ?1
                and resourceId = ?2
                and revokedAt is null
                and (expiresAt is null or expiresAt > ?3)
                """, resourceType, resourceId, now);
    }

    public boolean existsActiveGrant(ResourceType resourceType, UUID resourceId, List<UUID> subjectIds,
                                     Permission permission, Instant now) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return false;
        }
        return count("""
                resourceType = ?1
                and resourceId = ?2
                and subjectId in ?3
                and permission = ?4
                and revokedAt is null
                and (expiresAt is null or expiresAt > ?5)
                """, resourceType, resourceId, subjectIds, permission, now) > 0;
    }

    public int revokeActiveByGroupId(UUID groupId) {
        return update("revokedAt = CURRENT_TIMESTAMP where subjectType = ?1 and subjectId = ?2 and revokedAt is null",
                SubjectType.GROUP, groupId);
    }
}
