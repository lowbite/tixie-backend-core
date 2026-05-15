package com.tixie.issue.domain;

import com.tixie.issue.domain.model.IssueType;
import org.junit.jupiter.api.Test;

import static com.tixie.issue.domain.model.IssueType.*;
import static org.junit.jupiter.api.Assertions.*;

class IssueTypeTest {

    @Test
    void epic_allowedParentType_isEmpty() {
        assertTrue(EPIC.allowedParentType().isEmpty());
    }

    @Test
    void story_allowedParentType_isEpic() {
        assertEquals(EPIC, STORY.allowedParentType().orElseThrow());
    }

    @Test
    void task_allowedParentType_isStory() {
        assertEquals(STORY, TASK.allowedParentType().orElseThrow());
    }

    @Test
    void subtask_allowedParentType_isTask() {
        assertEquals(TASK, SUBTASK.allowedParentType().orElseThrow());
    }
}
