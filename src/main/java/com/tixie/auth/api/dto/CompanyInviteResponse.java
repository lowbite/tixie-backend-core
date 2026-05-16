package com.tixie.auth.api.dto;

import com.tixie.auth.UserRole;

import java.time.Instant;
import java.util.UUID;

public class CompanyInviteResponse {
    public UUID id;
    public UUID companyId;
    public String companyName;
    public String email;
    public UserRole role;
    public Instant expiresAt;
    public String token;
}
