package io.surisoft.capi.lb.repository;

import io.surisoft.capi.lb.schema.Api;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRepository extends JpaRepository<Api, String> {

}
