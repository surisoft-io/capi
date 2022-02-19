package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.swing.text.html.parser.Entity;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class ApiRepository { //extends JpaRepository<Api, String> {

    @Autowired(required = false)
    private EntityManager entityManager;

    public Collection<Api> findAll() {
        Query query = entityManager.createQuery("SELECT a FROM Api a");
        return query.getResultList();
    }

    public Optional<Api> findById(String apiId) {
        return Optional.ofNullable(entityManager.find(Api.class, apiId));
    }

    public void save(Api api) {
        entityManager.persist(api);
    }

    public void update(Api api) {
        entityManager.merge(api);
    }

    public List<Api> findByPublished(boolean published) {
        return null;
    }
}
