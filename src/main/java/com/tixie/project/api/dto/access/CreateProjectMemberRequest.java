package com.tixie.project.api.dto.access;

import com.tixie.authz.ProjectRole;
import com.tixie.authz.SubjectType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateProjectMemberRequest {
    @NotNull
    public SubjectType subjectType;

    @NotNull
    public UUID subjectId;

    @NotNull
    public ProjectRole role;
}
