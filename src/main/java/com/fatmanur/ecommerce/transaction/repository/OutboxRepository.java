package com.fatmanur.ecommerce.transaction.repository;

import com.fatmanur.ecommerce.transaction.entity.OutboxMessage;
import com.fatmanur.ecommerce.transaction.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findByStatus(OutboxStatus status);

}
