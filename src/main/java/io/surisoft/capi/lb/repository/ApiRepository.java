package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.swing.text.html.parser.Entity;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class ApiRepository {

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

    public Collection<Api> findByPublished(boolean published) {
        TypedQuery<Api> query = entityManager.createQuery("SELECT a FROM Api a WHERE a.published = :published" , Api.class);
        return query.setParameter("published", published).getResultList();
    }

    public void delete(Api api) {
        Api managed = entityManager.merge(api);
        entityManager.remove(managed);
    }
}
