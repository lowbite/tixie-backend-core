package com.tixie.issue.api.dto;

import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateIssueRequest {

    @NotBlank
    @Size(max = 255)
    public String title;

    public String description;

    @NotNull
    public IssueType type;

    public IssuePriority priority;

    @NotNull
    public UUID statusId;

    public UUID parentId;
}
