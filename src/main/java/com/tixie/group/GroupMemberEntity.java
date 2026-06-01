package com.tixie.group;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "group_members")
public class GroupMemberEntity extends PanacheEntityBase {

    @EmbeddedId
    public GroupMemberId id;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Embeddable
    public static class GroupMemberId implements Serializable {
        @Column(name = "group_id")
        public UUID groupId;

        @Column(name = "user_id")
        public UUID userId;

        public GroupMemberId() {
        }

        public GroupMemberId(UUID groupId, UUID userId) {
            this.groupId = groupId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GroupMemberId that)) {
                return false;
            }
            return Objects.equals(groupId, that.groupId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, userId);
        }
    }
}
