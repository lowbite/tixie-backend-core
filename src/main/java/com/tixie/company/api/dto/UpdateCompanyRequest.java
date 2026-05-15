package com.tixie.company.api.dto;

import jakarta.validation.constraints.Size;

public class UpdateCompanyRequest {

    @Size(max = 255)
    public String name;
}
