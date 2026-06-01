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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Groups")
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
    public List<GroupResponse> list() {
        var user = currentUser.require();
        return groupService.list(user.companyId).stream().map(this::toResponse).toList();
    }

    @POST
    @RequiresPermission(value = Permission.GROUP_CREATE, resource = ResourceType.COMPANY)
    public Response create(@Valid CreateGroupRequest req) {
        var user = currentUser.require();
        var group = groupService.create(user.companyId, user.id, req.name);
        return Response.status(Response.Status.CREATED).entity(toResponse(group)).build();
    }

    @GET
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_READ, resource = ResourceType.GROUP, idParam = "groupId")
    public GroupResponse get(@PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        return toResponse(groupService.getById(groupId, user.companyId));
    }

    @PATCH
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_UPDATE, resource = ResourceType.GROUP, idParam = "groupId")
    public GroupResponse update(@PathParam("groupId") UUID groupId, @Valid UpdateGroupRequest req) {
        var user = currentUser.require();
        return toResponse(groupService.update(groupId, user.companyId, req.name));
    }

    @DELETE
    @Path("/{groupId}")
    @RequiresPermission(value = Permission.GROUP_DELETE, resource = ResourceType.GROUP, idParam = "groupId")
    public Response delete(@PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        groupService.delete(groupId, user.companyId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{groupId}/members")
    @RequiresPermission(value = Permission.GROUP_READ, resource = ResourceType.GROUP, idParam = "groupId")
    public List<GroupMemberResponse> listMembers(@PathParam("groupId") UUID groupId) {
        var user = currentUser.require();
        return groupService.listMembers(groupId, user.companyId).stream().map(this::toMemberResponse).toList();
    }

    @PUT
    @Path("/{groupId}/members")
    @RequiresPermission(value = Permission.GROUP_MANAGE_MEMBERS, resource = ResourceType.GROUP, idParam = "groupId")
    public List<GroupMemberResponse> replaceMembers(@PathParam("groupId") UUID groupId,
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
