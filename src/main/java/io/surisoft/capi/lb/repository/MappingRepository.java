package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, String> {
    Optional<Mapping> findByRootContextAndHostnameAndPort(String rootContext, String hostname, int port);
}