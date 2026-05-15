package com.tixie.issue;

import com.tixie.issue.api.dto.*;
import com.tixie.issue.domain.model.IssuePriority;
import com.tixie.issue.domain.model.IssueType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueModelSmokeTest {

    @Test
    void entitiesEnumsAndDtos_areUsable() {
        var issue = new IssueEntity();
        issue.id = UUID.randomUUID();
        issue.issueKey = "PRJ-1";
        issue.type = IssueType.TASK;
        issue.priority = IssuePriority.HIGH;
        issue.createdAt = Instant.now();
        issue.updatedAt = Instant.now();
        issue.position = 2;

        var counter = new IssueKeyCounterEntity();
        counter.projectId = UUID.randomUUID();
        counter.lastSeq = 5;

        var create = new CreateIssueRequest();
        create.title = "T";
        create.type = IssueType.STORY;
        create.statusId = UUID.randomUUID();
        var patch = new PatchIssueRequest();
        patch.setDescription(null);
        var transition = new TransitionIssueRequest();
        transition.targetStatusId = UUID.randomUUID();
        var move = new MoveIssueRequest();
        move.targetStatusId = UUID.randomUUID();
        move.targetPosition = 1;

        var response = new IssueResponse();
        response.status = new IssueResponse.StatusRef(UUID.randomUUID(), "To Do");
        assertEquals("To Do", response.status.name());

        var board = new ProjectBoardResponse();
        board.columns = List.of(new ProjectBoardResponse.Column(
                UUID.randomUUID(), "To Do", 1, true,
                List.of(new ProjectBoardResponse.Card(issue.id, issue.issueKey, "T", null, issue.type, issue.priority, null, issue.position))
        ));
        assertEquals(1, board.columns.get(0).issues().size());
    }

    @Test
    void issuePriority_enumContainsExpectedValue() {
        assertEquals(IssuePriority.CRITICAL, IssuePriority.valueOf("CRITICAL"));
    }
}
