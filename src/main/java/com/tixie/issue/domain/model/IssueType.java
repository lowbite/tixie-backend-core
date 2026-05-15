package com.tixie.issue.domain.model;

import java.util.Optional;

public enum IssueType {
    EPIC, STORY, TASK, SUBTASK;

    public Optional<IssueType> allowedParentType() {
        return switch (this) {
            case EPIC    -> Optional.empty();
            case STORY   -> Optional.of(EPIC);
            case TASK    -> Optional.of(STORY);
            case SUBTASK -> Optional.of(TASK);
        };
    }
}
