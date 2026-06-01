package com.tixie.group;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepositoryBase<GroupMemberEntity, GroupMemberEntity.GroupMemberId> {

    public List<UUID> findGroupIdsByUserId(UUID userId) {
        return getEntityManager().createQuery("""
                        select gm.id.groupId
                        from GroupMemberEntity gm
                        join GroupEntity g on g.id = gm.id.groupId
                        where gm.id.userId = :userId
                          and g.deletedAt is null
                        """, UUID.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<GroupMemberEntity> findByGroupId(UUID groupId) {
        return list("id.groupId = ?1", groupId);
    }
}
