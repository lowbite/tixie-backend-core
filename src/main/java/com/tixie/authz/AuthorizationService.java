package com.tixie.authz;

import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.company.CompanyRepository;
import com.tixie.group.GroupRepository;
import com.tixie.group.GroupMemberRepository;
import com.tixie.issue.IssueRepository;
import com.tixie.project.ProjectEntity;
import com.tixie.project.ProjectRepository;
import com.tixie.project.access.ProjectMemberRepository;
import com.tixie.resourcegrant.ResourceGrantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuthorizationService {

    @Inject
    RolePermissions rolePermissions;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    IssueRepository issueRepository;

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    @Inject
    ProjectMemberRepository projectMemberRepository;

    @Inject
    ResourceGrantRepository resourceGrantRepository;

    public void require(UserEntity user, Permission permission, ResourceType resourceType, UUID resourceId) {
        if (!can(user, permission, resourceType, resourceId)) {
            throw new ForbiddenException("Missing permission " + permission);
        }
    }

    public boolean can(UserEntity user, Permission permission, ResourceType resourceType, UUID resourceId) {
        var context = resolve(resourceType, resourceId, user);
        if (!context.companyId().equals(user.companyId)) {
            throw notFound(resourceType, resourceId);
        }

        if (hasCompanyPermission(user, permission, context)) {
            return true;
        }

        var groupIds = groupMemberRepository.findGroupIdsByUserId(user.id);
        if (hasProjectPermission(user, groupIds, permission, context)) {
            return true;
        }

        return hasResourceGrant(user, groupIds, permission, context);
    }

    public ResourceContext resolve(ResourceType resourceType, UUID resourceId, UserEntity user) {
        if (resourceType == ResourceType.COMPANY) {
            UUID companyId = resourceId != null ? resourceId : user.companyId;
            companyRepository.findActiveById(companyId).orElseThrow(() -> notFound(resourceType, companyId));
            return new ResourceContext(ResourceType.COMPANY, companyId, companyId, null);
        }

        if (resourceType == ResourceType.GROUP) {
            var group = groupRepository.findActiveById(resourceId).orElseThrow(() -> notFound(resourceType, resourceId));
            return new ResourceContext(ResourceType.GROUP, resourceId, group.companyId, null);
        }

        if (resourceType == ResourceType.PROJECT) {
            var project = projectRepository.findActiveById(resourceId).orElseThrow(() -> notFound(resourceType, resourceId));
            return new ResourceContext(ResourceType.PROJECT, resourceId, project.companyId, project.id);
        }

        if (resourceType == ResourceType.ISSUE) {
            var issue = issueRepository.findActiveById(resourceId).orElseThrow(() -> notFound(resourceType, resourceId));
            var project = projectRepository.findActiveById(issue.projectId)
                    .orElseThrow(() -> notFound(ResourceType.PROJECT, issue.projectId));
            return new ResourceContext(ResourceType.ISSUE, resourceId, project.companyId, project.id);
        }

        throw new IllegalArgumentException("Unsupported resource type " + resourceType);
    }

    private boolean hasCompanyPermission(UserEntity user, Permission permission, ResourceContext context) {
        if (user.role == UserRole.OWNER || user.role == UserRole.ADMIN || context.projectId() == null) {
            return companyRoleHas(user.role, permission);
        }

        var project = projectRepository.findActiveById(context.projectId())
                .orElseThrow(() -> notFound(ResourceType.PROJECT, context.projectId()));
        if (project.accessMode == ProjectAccessMode.RESTRICTED) {
            return false;
        }

        return companyRoleHas(user.role, permission);
    }

    private boolean hasProjectPermission(UserEntity user, List<UUID> groupIds, Permission permission, ResourceContext context) {
        if (context.projectId() == null) {
            return false;
        }

        var memberships = projectMemberRepository.findActiveByProjectId(context.projectId());
        for (var membership : memberships) {
            boolean directUser = membership.subjectType == SubjectType.USER && membership.subjectId.equals(user.id);
            boolean groupMember = membership.subjectType == SubjectType.GROUP && groupIds.contains(membership.subjectId);
            if ((directUser || groupMember) && projectRoleHas(membership.role, permission)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasResourceGrant(UserEntity user, List<UUID> groupIds, Permission permission, ResourceContext context) {
        var grants = resourceGrantRepository.findActiveByResource(context.type(), context.resourceId(), Instant.now());
        for (var grant : grants) {
            if (!permissionImplies(grant.permission, permission)) {
                continue;
            }
            boolean directUser = grant.subjectType == SubjectType.USER && grant.subjectId.equals(user.id);
            boolean groupMember = grant.subjectType == SubjectType.GROUP && groupIds.contains(grant.subjectId);
            if (directUser || groupMember) {
                return true;
            }
        }
        return false;
    }

    private boolean companyRoleHas(UserRole role, Permission requested) {
        return EnumSet.allOf(Permission.class).stream()
                .anyMatch(granted -> permissionImplies(granted, requested)
                        && rolePermissions.companyRoleHas(role, granted));
    }

    private boolean projectRoleHas(ProjectRole role, Permission requested) {
        return EnumSet.allOf(Permission.class).stream()
                .anyMatch(granted -> permissionImplies(granted, requested)
                        && rolePermissions.projectRoleHas(role, granted));
    }

    private boolean permissionImplies(Permission granted, Permission requested) {
        if (granted == requested) {
            return true;
        }

        if (requested == Permission.ISSUE_READ) {
            return switch (granted) {
                case ISSUE_UPDATE, ISSUE_DELETE, ISSUE_TRANSITION, ISSUE_ASSIGN, ISSUE_MANAGE_GRANTS -> true;
                default -> false;
            };
        }

        if (requested == Permission.PROJECT_READ) {
            return switch (granted) {
                case PROJECT_UPDATE, PROJECT_DELETE, PROJECT_MANAGE_MEMBERS, PROJECT_MANAGE_STATUSES -> true;
                default -> false;
            };
        }

        if (requested == Permission.COMPANY_READ) {
            return switch (granted) {
                case COMPANY_UPDATE, COMPANY_DELETE, COMPANY_MANAGE_USERS, COMPANY_MANAGE_ROLES -> true;
                default -> false;
            };
        }

        if (requested == Permission.GROUP_READ) {
            return switch (granted) {
                case GROUP_UPDATE, GROUP_DELETE, GROUP_MANAGE_MEMBERS -> true;
                default -> false;
            };
        }

        return false;
    }

    private NotFoundException notFound(ResourceType resourceType, UUID resourceId) {
        return new NotFoundException(resourceType + " '" + resourceId + "' not found");
    }
}
