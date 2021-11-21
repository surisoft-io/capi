package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, String> {
}
