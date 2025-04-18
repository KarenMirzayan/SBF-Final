package kz.kbtu.message_relay.repository;

import kz.kbtu.message_relay.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("SELECT e FROM OutboxEvent e ORDER BY e.createdAt ASC")
    List<OutboxEvent> findAllOrderByCreatedAtAsc();
}