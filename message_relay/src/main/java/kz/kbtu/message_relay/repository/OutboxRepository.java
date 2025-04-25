package kz.kbtu.message_relay.repository;

import kz.kbtu.message_relay.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "SELECT * FROM outbox WHERE id = :id FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findByIdWithLock(@Param("id") UUID id);

    @Query(value = "SELECT * FROM outbox ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findWithLockOrderByCreatedAtAsc(@Param("limit") int limit);
}