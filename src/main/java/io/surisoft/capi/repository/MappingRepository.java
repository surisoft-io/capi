package io.surisoft.capi.repository;

import io.surisoft.capi.schema.Mapping;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class MappingRepository {

    @Autowired(required = false)
    private EntityManager entityManager;

    public void delete(Mapping mapping) {
        Mapping managed = entityManager.merge(mapping);
        entityManager.remove(managed);
    }
}