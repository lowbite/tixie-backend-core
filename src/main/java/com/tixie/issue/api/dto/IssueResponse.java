package com.tixie.issue.api.dto;

import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;

import java.time.Instant;
import java.util.UUID;

public class IssueResponse {

    public UUID id;
    public String issueKey;
    public String title;
    public String description;
    public IssueType type;
    public IssuePriority priority;
    public StatusRef status;
    public UUID projectId;
    public UUID parentId;
    public Instant createdAt;
    public Instant updatedAt;

    public record StatusRef(UUID id, String name) {}
}
