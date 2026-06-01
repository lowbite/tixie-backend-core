package com.tixie.project.access;

import com.tixie.authz.SubjectType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProjectMemberRepository implements PanacheRepositoryBase<ProjectMemberEntity, UUID> {

    public List<ProjectMemberEntity> findActiveByProjectId(UUID projectId) {
        return list("projectId = ?1 and revokedAt is null", projectId);
    }

    public List<ProjectMemberEntity> findActiveByProjectIdAndSubjects(UUID projectId, List<UUID> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        return list("projectId = ?1 and subjectId in ?2 and revokedAt is null", projectId, subjectIds);
    }

    public Optional<ProjectMemberEntity> findActiveByProjectAndSubject(UUID projectId, SubjectType subjectType, UUID subjectId) {
        return find("projectId = ?1 and subjectType = ?2 and subjectId = ?3 and revokedAt is null",
                projectId, subjectType, subjectId).firstResultOptional();
    }

    public int revokeActiveByGroupId(UUID groupId) {
        return update("revokedAt = CURRENT_TIMESTAMP where subjectType = ?1 and subjectId = ?2 and revokedAt is null",
                SubjectType.GROUP, groupId);
    }
}
