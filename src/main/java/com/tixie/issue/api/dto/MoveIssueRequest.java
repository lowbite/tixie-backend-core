package com.tixie.issue.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class MoveIssueRequest {

    @NotNull
    public UUID targetStatusId;

    public Integer targetPosition;
}
