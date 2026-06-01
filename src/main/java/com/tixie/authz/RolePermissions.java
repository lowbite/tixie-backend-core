package com.tixie.authz;

import com.tixie.auth.UserRole;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

@ApplicationScoped
public class RolePermissions {

    private final EnumMap<UserRole, Set<Permission>> companyPermissions = new EnumMap<>(UserRole.class);
    private final EnumMap<ProjectRole, Set<Permission>> projectPermissions = new EnumMap<>(ProjectRole.class);

    public RolePermissions() {
        companyPermissions.put(UserRole.OWNER, EnumSet.allOf(Permission.class));
        companyPermissions.put(UserRole.ADMIN, EnumSet.complementOf(EnumSet.of(
                Permission.COMPANY_DELETE,
                Permission.COMPANY_MANAGE_ROLES
        )));
        companyPermissions.put(UserRole.MEMBER, EnumSet.of(
                Permission.COMPANY_READ,
                Permission.PROJECT_READ,
                Permission.ISSUE_READ,
                Permission.ISSUE_CREATE,
                Permission.ISSUE_UPDATE,
                Permission.ISSUE_TRANSITION
        ));
        companyPermissions.put(UserRole.VIEWER, EnumSet.of(
                Permission.COMPANY_READ,
                Permission.PROJECT_READ,
                Permission.ISSUE_READ
        ));

        projectPermissions.put(ProjectRole.PROJECT_MANAGER, EnumSet.of(
                Permission.PROJECT_READ,
                Permission.PROJECT_UPDATE,
                Permission.PROJECT_MANAGE_MEMBERS,
                Permission.PROJECT_MANAGE_STATUSES,
                Permission.ISSUE_READ,
                Permission.ISSUE_CREATE,
                Permission.ISSUE_UPDATE,
                Permission.ISSUE_DELETE,
                Permission.ISSUE_TRANSITION,
                Permission.ISSUE_ASSIGN,
                Permission.ISSUE_MANAGE_GRANTS
        ));
        projectPermissions.put(ProjectRole.PROJECT_CONTRIBUTOR, EnumSet.of(
                Permission.PROJECT_READ,
                Permission.ISSUE_READ,
                Permission.ISSUE_CREATE,
                Permission.ISSUE_UPDATE,
                Permission.ISSUE_TRANSITION
        ));
        projectPermissions.put(ProjectRole.PROJECT_VIEWER, EnumSet.of(
                Permission.PROJECT_READ,
                Permission.ISSUE_READ
        ));
    }

    public boolean companyRoleHas(UserRole role, Permission permission) {
        return companyPermissions.getOrDefault(role, Set.of()).contains(permission);
    }

    public boolean projectRoleHas(ProjectRole role, Permission permission) {
        return projectPermissions.getOrDefault(role, Set.of()).contains(permission);
    }
}
