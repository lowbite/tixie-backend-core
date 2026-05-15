package com.tixie.issue.api.dto;

import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;

import java.util.List;
import java.util.UUID;

public class ProjectBoardResponse {

    public UUID projectId;
    public List<Column> columns;

    public record Column(UUID statusId, String statusName, int displayOrder, boolean isDefault, List<Card> issues) {}

    public record Card(
            UUID id,
            String issueKey,
            String title,
            String description,
            IssueType type,
            IssuePriority priority,
            UUID parentId,
            long position
    ) {}
}
