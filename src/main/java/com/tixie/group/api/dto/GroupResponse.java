package com.tixie.group.api.dto;

import java.time.Instant;
import java.util.UUID;

public class GroupResponse {
    public UUID id;
    public UUID companyId;
    public String name;
    public Instant createdAt;
}
