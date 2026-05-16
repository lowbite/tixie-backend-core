package com.tixie.auth.api.dto;

import com.tixie.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCompanyInviteRequest {
    @Email
    @NotBlank
    public String email;

    @NotNull
    public UserRole role;
}
