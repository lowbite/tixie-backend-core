package com.tixie.company.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCompanyRequest {

    @NotBlank
    @Size(max = 255)
    public String name;
}
