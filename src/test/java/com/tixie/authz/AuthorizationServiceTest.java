package com.tixie.authz;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.company.CompanyRepository;
import com.tixie.group.GroupMemberRepository;
import com.tixie.group.GroupRepository;
import com.tixie.issue.IssueRepository;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import com.tixie.project.access.ProjectMemberEntity;
import com.tixie.project.access.ProjectMemberRepository;
import com.tixie.resourcegrant.ResourceGrantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {

    @Test
    void restrictedProject_requiresProjectMembershipForCompanyMember() {
        var service = service();
        var user = user(UserRole.MEMBER);
        var project = project(user.companyId, ProjectAccessMode.RESTRICTED);
        when(service.projectRepository.findActiveById(project.id)).thenReturn(Optional.of(project));
        when(service.groupMemberRepository.findGroupIdsByUserId(user.id)).thenReturn(List.of());
        when(service.projectMemberRepository.findActiveByProjectId(project.id)).thenReturn(List.of());

        assertFalse(service.can(user, Permission.PROJECT_READ, ResourceType.PROJECT, project.id));
    }

    @Test
    void groupProjectRole_grantsPermission() {
        var service = service();
        var user = user(UserRole.MEMBER);
        UUID groupId = UUID.randomUUID();
        var project = project(user.companyId, ProjectAccessMode.RESTRICTED);
        var membership = new ProjectMemberEntity();
        membership.projectId = project.id;
        membership.subjectType = SubjectType.GROUP;
        membership.subjectId = groupId;
        membership.role = ProjectRole.PROJECT_MANAGER;

        when(service.projectRepository.findActiveById(project.id)).thenReturn(Optional.of(project));
        when(service.groupMemberRepository.findGroupIdsByUserId(user.id)).thenReturn(List.of(groupId));
        when(service.projectMemberRepository.findActiveByProjectId(project.id)).thenReturn(List.of(membership));

        assertTrue(service.can(user, Permission.PROJECT_MANAGE_MEMBERS, ResourceType.PROJECT, project.id));
    }

    private AuthorizationService service() {
        var service = new AuthorizationService();
        service.rolePermissions = new RolePermissions();
        service.companyRepository = mock(CompanyRepository.class);
        service.projectRepository = mock(ProjectRepository.class);
        service.issueRepository = mock(IssueRepository.class);
        service.groupRepository = mock(GroupRepository.class);
        service.groupMemberRepository = mock(GroupMemberRepository.class);
        service.projectMemberRepository = mock(ProjectMemberRepository.class);
        service.resourceGrantRepository = mock(ResourceGrantRepository.class);
        when(service.resourceGrantRepository.findActiveByResource(any(), any(), any())).thenReturn(List.of());
        return service;
    }

    private UserEntity user(UserRole role) {
        var user = new UserEntity();
        user.id = UUID.randomUUID();
        user.companyId = UUID.randomUUID();
        user.role = role;
        return user;
    }

    private ProjectEntity project(UUID companyId, ProjectAccessMode accessMode) {
        var project = new ProjectEntity();
        project.id = UUID.randomUUID();
        project.companyId = companyId;
        project.accessMode = accessMode;
        return project;
    }
}
