package com.payguard.infrastructure.persistence;

import com.payguard.domain.shared.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Senaryo Spring Data JPA repository'si.
 *
 * Metot adından sorgu türetilir: findByProductTypeAndModule -> WHERE product_type=? AND module=?
 * (sonuç ScenarioCatalog'ta cache'lenir).
 */
public interface ScenarioJpaRepository extends JpaRepository<ScenarioRow, Long> {

    List<ScenarioRow> findByProductTypeAndModule(ProductType productType, int module);

    boolean existsByProductType(ProductType productType);
}
