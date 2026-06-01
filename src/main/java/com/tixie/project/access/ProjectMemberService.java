package com.tixie.project.access;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRepository;
import com.tixie.authz.ProjectRole;
import com.tixie.authz.SubjectType;
import com.tixie.group.GroupRepository;
import com.tixie.project.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectMemberService {

    @Inject
    ProjectMemberRepository projectMemberRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupRepository groupRepository;

    public List<ProjectMemberEntity> list(UUID projectId) {
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
        return projectMemberRepository.findActiveByProjectId(projectId);
    }

    @Transactional
    public ProjectMemberEntity create(UUID projectId, SubjectType subjectType, UUID subjectId,
                                      ProjectRole role, UserEntity creator) {
        var project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));
        validateSubject(project.companyId, subjectType, subjectId);

        var existing = projectMemberRepository.findActiveByProjectAndSubject(projectId, subjectType, subjectId);
        if (existing.isPresent()) {
            existing.get().role = role;
            return existing.get();
        }

        var member = new ProjectMemberEntity();
        member.projectId = projectId;
        member.subjectType = subjectType;
        member.subjectId = subjectId;
        member.role = role;
        member.createdByUserId = creator.id;
        member.createdAt = Instant.now();
        projectMemberRepository.persist(member);
        return member;
    }

    @Transactional
    public ProjectMemberEntity update(UUID projectId, UUID memberId, ProjectRole role) {
        var member = getActive(projectId, memberId);
        member.role = role;
        return member;
    }

    @Transactional
    public void delete(UUID projectId, UUID memberId) {
        var member = getActive(projectId, memberId);
        member.revokedAt = Instant.now();
    }

    private ProjectMemberEntity getActive(UUID projectId, UUID memberId) {
        return projectMemberRepository.findByIdOptional(memberId)
                .filter(member -> member.projectId.equals(projectId) && member.revokedAt == null)
                .orElseThrow(() -> new NotFoundException("Project member '" + memberId + "' not found"));
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
