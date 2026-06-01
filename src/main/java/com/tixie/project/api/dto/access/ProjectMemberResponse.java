package com.tixie.project.api.dto.access;

import com.tixie.authz.ProjectRole;
import com.tixie.authz.SubjectType;

import java.time.Instant;
import java.util.UUID;

public class ProjectMemberResponse {
    public UUID id;
    public UUID projectId;
    public SubjectType subjectType;
    public UUID subjectId;
    public ProjectRole role;
    public Instant createdAt;
}
