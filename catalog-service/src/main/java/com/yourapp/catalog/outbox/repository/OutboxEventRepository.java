package com.yourapp.catalog.outbox.repository;

import com.yourapp.catalog.outbox.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEventEntity> findTop20ByPublishedAtIsNullOrderByCreatedAtAsc();
}
