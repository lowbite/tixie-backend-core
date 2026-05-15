package com.tixie.project.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProjectResponse {

    public UUID id;
    public UUID companyId;
    public String name;
    public String key;
    public List<StatusRef> statuses;
    public Instant createdAt;

    public record StatusRef(UUID id, String name, int displayOrder, boolean isDefault) {}
}
