package com.tixie.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProjectStatusRequest {

    @NotBlank
    @Size(max = 100)
    public String name;

    public Integer displayOrder;

    public Boolean isDefault;
}
