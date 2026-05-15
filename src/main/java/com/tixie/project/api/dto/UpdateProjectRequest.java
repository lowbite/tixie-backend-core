package com.tixie.project.api.dto;

import jakarta.validation.constraints.Size;

public class UpdateProjectRequest {

    @Size(max = 255)
    public String name;
}
