package com.tixie.group.api.dto;

import jakarta.validation.constraints.Size;

public class UpdateGroupRequest {
    @Size(max = 255)
    public String name;
}
