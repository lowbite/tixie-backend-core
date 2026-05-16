package com.tixie.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCompanyOnboardingRequest {
    @NotBlank
    @Size(max = 255)
    public String companyName;
}
