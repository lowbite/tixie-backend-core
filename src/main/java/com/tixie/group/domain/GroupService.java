package com.tixie.group.domain;

import com.tixie.auth.UserRepository;
import com.tixie.group.GroupEntity;
import com.tixie.group.GroupMemberEntity;
import com.tixie.group.GroupMemberRepository;
import com.tixie.group.GroupRepository;
import com.tixie.project.access.ProjectMemberRepository;
import com.tixie.resourcegrant.ResourceGrantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GroupService {

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ProjectMemberRepository projectMemberRepository;

    @Inject
    ResourceGrantRepository resourceGrantRepository;

    public List<GroupEntity> list(UUID companyId) {
        return groupRepository.findActiveByCompanyId(companyId);
    }

    public GroupEntity getById(UUID groupId, UUID companyId) {
        return groupRepository.findActiveById(groupId)
                .filter(group -> group.companyId.equals(companyId))
                .orElseThrow(() -> new NotFoundException("Group '" + groupId + "' not found"));
    }

    @Transactional
    public GroupEntity create(UUID companyId, UUID createdByUserId, String name) {
        var group = new GroupEntity();
        group.companyId = companyId;
        group.name = name;
        group.createdByUserId = createdByUserId;
        group.createdAt = Instant.now();
        groupRepository.persist(group);
        return group;
    }

    @Transactional
    public GroupEntity update(UUID groupId, UUID companyId, String name) {
        var group = getById(groupId, companyId);
        if (name != null) {
            group.name = name;
        }
        return group;
    }

    @Transactional
    public void delete(UUID groupId, UUID companyId) {
        var group = getById(groupId, companyId);
        projectMemberRepository.revokeActiveByGroupId(groupId);
        resourceGrantRepository.revokeActiveByGroupId(groupId);
        group.deletedAt = Instant.now();
    }

    public List<GroupMemberEntity> listMembers(UUID groupId, UUID companyId) {
        getById(groupId, companyId);
        return groupMemberRepository.findByGroupId(groupId);
    }

    @Transactional
    public List<GroupMemberEntity> replaceMembers(UUID groupId, UUID companyId, List<UUID> userIds) {
        getById(groupId, companyId);
        groupMemberRepository.delete("id.groupId", groupId);
        for (UUID userId : userIds) {
            userRepository.findActiveByIdAndCompanyId(userId, companyId)
                    .orElseThrow(() -> new NotFoundException("User '" + userId + "' not found"));
            var member = new GroupMemberEntity();
            member.id = new GroupMemberEntity.GroupMemberId(groupId, userId);
            member.createdAt = Instant.now();
            groupMemberRepository.persist(member);
        }
        return groupMemberRepository.findByGroupId(groupId);
    }
}
