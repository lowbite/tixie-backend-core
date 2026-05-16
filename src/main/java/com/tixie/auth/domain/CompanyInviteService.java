package com.tixie.auth.domain;

import com.tixie.auth.CompanyInviteEntity;
import com.tixie.auth.CompanyInviteRepository;
import com.tixie.auth.UserEntity;
import com.tixie.auth.UserRole;
import com.tixie.company.CompanyEntity;
import com.tixie.company.CompanyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class CompanyInviteService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    @Inject
    CompanyInviteRepository inviteRepository;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    UserService userService;

    @Inject
    InviteTokenService inviteTokenService;

    @Transactional
    public CreatedInvite create(UserEntity creator, String email, UserRole role) {
        if (role == UserRole.OWNER) {
            throw new BadRequestException("Owner role cannot be assigned by invite");
        }

        var company = companyRepository.findActiveById(creator.companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + creator.companyId + "' not found"));
        if (userService.activeEmailExists(email)) {
            throw new ClientErrorException("Email is already linked to a user", Response.Status.CONFLICT);
        }

        String token = inviteTokenService.generateToken();
        var now = Instant.now();
        var invite = new CompanyInviteEntity();
        invite.companyId = creator.companyId;
        invite.email = email.trim().toLowerCase();
        invite.role = role;
        invite.tokenHash = inviteTokenService.hash(token);
        invite.expiresAt = now.plus(INVITE_TTL);
        invite.createdByUserId = creator.id;
        invite.createdAt = now;
        inviteRepository.persist(invite);

        return new CreatedInvite(invite, company, token);
    }

    public InviteDetails get(String token) {
        var invite = findPending(token);
        var company = companyRepository.findActiveById(invite.companyId)
                .orElseThrow(() -> new NotFoundException("Company '" + invite.companyId + "' not found"));
        return new InviteDetails(invite, company);
    }

    @Transactional
    public UserEntity accept(String token, KeycloakIdentity identity) {
        var invite = findPending(token);
        if (!invite.email.equalsIgnoreCase(identity.email())) {
            throw new ClientErrorException("Invite email does not match authenticated user", Response.Status.CONFLICT);
        }

        var user = userService.create(identity, invite.companyId, invite.role);
        invite.acceptedAt = Instant.now();
        return user;
    }

    private CompanyInviteEntity findPending(String token) {
        return inviteRepository.findPendingByTokenHash(inviteTokenService.hash(token), Instant.now())
                .orElseThrow(() -> new NotFoundException("Invite not found"));
    }

    public record CreatedInvite(CompanyInviteEntity invite, CompanyEntity company, String token) {
    }

    public record InviteDetails(CompanyInviteEntity invite, CompanyEntity company) {
    }
}
