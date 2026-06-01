package com.tixie.project.api.dto.access;

import com.tixie.authz.ProjectRole;
import jakarta.validation.constraints.NotNull;

public class UpdateProjectMemberRequest {
    @NotNull
    public ProjectRole role;
}
