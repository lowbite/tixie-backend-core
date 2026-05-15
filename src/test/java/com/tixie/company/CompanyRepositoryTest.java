package com.tixie.company;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CompanyRepositoryTest {

    @Test
    void findActiveById_delegatesToPanacheFind() {
        var repo = spy(new CompanyRepository());
        @SuppressWarnings("unchecked")
        PanacheQuery<CompanyEntity> query = mock(PanacheQuery.class);
        var entity = new CompanyEntity();
        UUID id = UUID.randomUUID();
        doReturn(query).when(repo).find("id = ?1 and deletedAt is null", id);
        when(query.firstResultOptional()).thenReturn(Optional.of(entity));

        var result = repo.findActiveById(id);
        assertSame(entity, result.orElseThrow());
    }
}
