package com.fraud.infrastructure.persistence;

import com.fraud.domain.shared.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for scenarios.
 *
 * The query is derived from the method name: findByProductTypeAndModule -> WHERE product_type=? AND module=?
 * (the result is cached in ScenarioCatalog).
 */
public interface ScenarioJpaRepository extends JpaRepository<ScenarioRow, Long> {

    List<ScenarioRow> findByProductTypeAndModule(ProductType productType, int module);

    boolean existsByProductType(ProductType productType);
}
