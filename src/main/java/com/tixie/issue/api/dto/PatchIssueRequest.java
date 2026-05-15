package com.tixie.issue.api.dto;

import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PatchIssueRequest {

    @Size(max = 255)
    public String title;

    public String description;

    public IssueType type;

    public IssuePriority priority;

    public UUID statusId;

    public UUID parentId;
}
