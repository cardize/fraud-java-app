package com.fraud.infrastructure.persistence;

import com.fraud.domain.shared.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Spring Data JPA repository for scenarios.
 *
 * The query is derived from the method name: findByProductTypeAndModule -> WHERE product_type=? AND module=?
 * (the result is cached in ScenarioCatalog).
 *
 * JpaSpecificationExecutor powers the paged/filtered admin listing: optional filters are composed
 * as criteria predicates ONLY when present — no null-parameter tricks in JPQL (those behave
 * differently across databases).
 */
public interface ScenarioJpaRepository extends JpaRepository<ScenarioRow, Long>,
        JpaSpecificationExecutor<ScenarioRow> {

    List<ScenarioRow> findByProductTypeAndModule(ProductType productType, int module);

    boolean existsByProductType(ProductType productType);
}
