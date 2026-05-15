package com.tixie.issue.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TransitionIssueRequest {

    @NotNull
    public UUID targetStatusId;
}
