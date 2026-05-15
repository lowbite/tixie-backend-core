package com.tixie.company;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CompanyRepository implements PanacheRepositoryBase<CompanyEntity, UUID> {

    public Optional<CompanyEntity> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    public List<CompanyEntity> listActive(int page, int size) {
        return find("deletedAt is null")
                .page(Page.of(page, size))
                .list();
    }
}
