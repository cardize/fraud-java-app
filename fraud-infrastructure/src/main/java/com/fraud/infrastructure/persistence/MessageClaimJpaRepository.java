package com.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageClaimJpaRepository extends JpaRepository<MessageClaim, Long> {
}
