package com.tixie.auth.api.dto;

import com.tixie.auth.UserRole;

import java.util.UUID;

public class CurrentUserResponse {
    public UUID id;
    public UUID companyId;
    public String email;
    public String displayName;
    public UserRole role;
}
