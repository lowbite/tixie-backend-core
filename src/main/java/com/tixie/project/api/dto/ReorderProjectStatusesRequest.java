package com.tixie.project.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ReorderProjectStatusesRequest {

    @NotEmpty
    public List<@NotNull UUID> statusIds;
}
