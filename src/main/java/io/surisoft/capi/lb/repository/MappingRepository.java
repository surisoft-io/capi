package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Mapping;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.Optional;

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