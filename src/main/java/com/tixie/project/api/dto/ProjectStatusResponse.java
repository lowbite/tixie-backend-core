package com.tixie.project.api.dto;

import java.util.UUID;

public class ProjectStatusResponse {
    public UUID id;
    public UUID projectId;
    public String name;
    public int displayOrder;
    public boolean isDefault;
}
