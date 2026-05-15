package com.tixie.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest {

    @NotBlank
    @Size(max = 255)
    public String name;

    @NotBlank
    @Pattern(
            regexp = "^[A-Z][A-Z0-9]{1,9}$",
            message = "Key must be 2–10 uppercase letters/digits and start with a letter"
    )
    public String key;
}
