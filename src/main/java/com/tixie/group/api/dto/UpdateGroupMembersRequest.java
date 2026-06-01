package com.tixie.group.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class UpdateGroupMembersRequest {
    @NotNull
    public List<UUID> userIds;
}
