package com.tixie.issue.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PatchIssueRequest {

    @Size(max = 255)
    private String title;

    private String description;

    private IssueType type;

    private IssuePriority priority;

    private UUID statusId;

    private UUID parentId;

    @JsonIgnore
    private boolean titleSet;
    @JsonIgnore
    private boolean descriptionSet;
    @JsonIgnore
    private boolean typeSet;
    @JsonIgnore
    private boolean prioritySet;
    @JsonIgnore
    private boolean statusIdSet;
    @JsonIgnore
    private boolean parentIdSet;

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titleSet = true;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    public IssueType getType() {
        return type;
    }

    @JsonSetter("type")
    public void setType(IssueType type) {
        this.type = type;
        this.typeSet = true;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    @JsonSetter("priority")
    public void setPriority(IssuePriority priority) {
        this.priority = priority;
        this.prioritySet = true;
    }

    public UUID getStatusId() {
        return statusId;
    }

    @JsonSetter("statusId")
    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
        this.statusIdSet = true;
    }

    public UUID getParentId() {
        return parentId;
    }

    @JsonSetter("parentId")
    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.parentIdSet = true;
    }

    public boolean isTitleSet() {
        return titleSet;
    }

    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    public boolean isTypeSet() {
        return typeSet;
    }

    public boolean isPrioritySet() {
        return prioritySet;
    }

    public boolean isStatusIdSet() {
        return statusIdSet;
    }

    public boolean isParentIdSet() {
        return parentIdSet;
    }
}
