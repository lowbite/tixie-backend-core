package com.tixie.group.api;

import com.tixie.auth.UserRepository;
import com.tixie.auth.domain.CurrentUser;
import com.tixie.authz.*;
import com.tixie.group.GroupEntity;
import com.tixie.group.GroupMemberEntity;
import com.tixie.group.api.dto.*;
import com.tixie.group.domain.GroupService;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Groups")
@SecurityRequirement(name = "bearerAuth")
@RunOnVirtualThread
@Authenticated
public class GroupResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    GroupService groupService;

    @Inject
    UserRepository userRepository;

    @GET
    @RequiresPermission(value = Permission.GROUP_READ, resource = ResourceType.COMPANY)
    @Operation(summary = "List company groups")
    @APIResponse(responseCode = "200", description = "List of groups",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GroupResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    public List<GroupResponse> list() {
        var user = currentUser.require();
        return groupService.list(user.companyId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.GROUP_CREATE, resource = ResourceType.COMPANY)
    @Operation(summary = "Create a group")
    @APIResponse(responseCode = "201", description = "Group created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GroupResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    public Response create(@Valid CreateGroupRequest req) {
        var user = currentUser.require();
        var group = groupService.create(user.companyId, user.id, req.name);
        return Response.status(Response.Status.CREATED).entity(toResponse(group)).build();
    }

    @GET
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_READ, resource = ResourceType.GROUP, idParam = "groupId")
    @Operation(summary = "Get a group by ID")
    @APIResponse(responseCode = "200", description = "Group found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GroupResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Group not found")
    public GroupResponse get(@Parameter(description = "Group ID") @PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        return toResponse(groupService.getById(groupId, user.companyId));
    }

    @PATCH
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_UPDATE, resource = ResourceType.GROUP, idParam = "groupId")
    @Operation(summary = "Partially update a group")
    @APIResponse(responseCode = "200", description = "Group updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GroupResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Group not found")
    public GroupResponse update(@Parameter(description = "Group ID") @PathParam("groupId") UUID groupId,
                                @Valid UpdateGroupRequest req) {
        var user = currentUser.require();
        return toResponse(groupService.update(groupId, user.companyId, req.name));
    }

    @DELETE
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_DELETE, resource = ResourceType.GROUP, idParam = "groupId")
    @Operation(summary = "Delete a group")
    @APIResponse(responseCode = "204", description = "Group deleted")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Group not found")
    public Response delete(@Parameter(description = "Group ID") @PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        groupService.delete(groupId, user.companyId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{groupId}/members")
    @RequiresPermission(value = Permission.GROUP_READ, resource = ResourceType.GROUP, idParam = "groupId")
    @Operation(summary = "List group members")
    @APIResponse(responseCode = "200", description = "List of group members",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GroupMemberResponse.class)))
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Group not found")
    public List<GroupMemberResponse> listMembers(@Parameter(description = "Group ID") @PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        return groupService.listMembers(groupId, user.companyId).stream().map(this::toMemberResponse).toList();
    }

    @PUT
    @Path("/{groupId}/members")
    @RequiresPermission(value = Permission.GROUP_MANAGE_MEMBERS, resource = ResourceType.GROUP, idParam = "groupId")
    @Operation(summary = "Replace group members")
    @APIResponse(responseCode = "200", description = "Updated list of group members",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = GroupMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Authentication required")
    @APIResponse(responseCode = "403", description = "Insufficient permissions")
    @APIResponse(responseCode = "404", description = "Group not found")
    public List<GroupMemberResponse> replaceMembers(@Parameter(description = "Group ID") @PathParam("groupId") UUID groupId,
                                                    @Valid UpdateGroupMembersRequest req) {
        var user = currentUser.require();
        return groupService.replaceMembers(groupId, user.companyId, req.userIds).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private GroupResponse toResponse(GroupEntity group) {
        var response = new GroupResponse();
        response.id = group.id;
        response.companyId = group.companyId;
        response.name = group.name;
        response.createdAt = group.createdAt;
        return response;
    }

    private GroupMemberResponse toMemberResponse(GroupMemberEntity member) {
        var user = userRepository.findById(member.id.userId);
        var response = new GroupMemberResponse();
        response.userId = member.id.userId;
        response.email = user == null ? null : user.email;
        response.displayName = user == null ? null : user.displayName;
        return response;
    }
}
