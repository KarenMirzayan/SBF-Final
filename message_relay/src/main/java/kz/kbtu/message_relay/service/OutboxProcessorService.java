package kz.kbtu.message_relay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.message_relay.model.OutboxEvent;
import kz.kbtu.message_relay.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OutboxProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessorService.class);
    private static final String TOPIC = "order-events";
    private static final int BATCH_SIZE = 5; // Process up to 5 events per batch

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxProcessorService(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processEventById(UUID eventId) {
        List<OutboxEvent> events = outboxRepository.findByIdWithLock(eventId);
        if (events.isEmpty()) {
            logger.debug("Event {} not found or already locked, skipping", eventId);
            return;
        }
        OutboxEvent event = events.getFirst();
        processEvent(event);
    }

    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findWithLockOrderByCreatedAtAsc(BATCH_SIZE);
        if (events.isEmpty()) {
            logger.debug("No pending events found in outbox");
            return;
        }
        logger.info("Processing {} pending events with FOR UPDATE SKIP LOCKED", events.size());
        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            logger.info("Processing outbox event with ID: {}", event.getId());
            kafkaTemplate.send(TOPIC, event.getAggregateId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published event {} to Kafka with offset {}",
                                    event.getId(), result.getRecordMetadata().offset());
                            outboxRepository.delete(event);
                            logger.info("Deleted outbox event with ID: {}", event.getId());
                        } else {
                            logger.error("Failed to publish event {} to Kafka: {}", event.getId(), ex.getMessage());
                            // Event remains in outbox for retry
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event {} to JSON: {}", event.getId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing event {}: {}", event.getId(), e.getMessage());
        }
    }
}