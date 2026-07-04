package com.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {

    /** The most recent entries for the admin read endpoint (bounded — never loads the whole table). */
    List<AuditLog> findTop100ByOrderByOccurredAtDesc();
}
