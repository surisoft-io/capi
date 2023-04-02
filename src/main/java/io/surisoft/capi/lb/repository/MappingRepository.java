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

    public Optional<Mapping> findByRootContextAndHostnameAndPort(String rootContext, String hostname, int port) {
        TypedQuery<Mapping> query = entityManager.createQuery("SELECT m FROM Mapping m WHERE m.rootContext = :rootContext and m.hostname = :hostname and port = :port" , Mapping.class);
        query.setParameter("rootContext", rootContext);
        query.setParameter("hostname", hostname);
        query.setParameter("port", port);
        return Optional.ofNullable(query.getSingleResult());
    }

    public void delete(Mapping mapping) {
        Mapping managed = entityManager.merge(mapping);
        entityManager.remove(managed);
    }
}