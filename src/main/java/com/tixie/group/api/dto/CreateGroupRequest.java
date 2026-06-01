package com.tixie.group.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupRequest {
    @NotBlank
    @Size(max = 255)
    public String name;
}
