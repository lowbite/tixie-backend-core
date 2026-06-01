package com.tixie.group;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GroupRepository implements PanacheRepositoryBase<GroupEntity, UUID> {

    public List<GroupEntity> findActiveByCompanyId(UUID companyId) {
        return list("companyId = ?1 and deletedAt is null", companyId);
    }

    public Optional<GroupEntity> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }
}
