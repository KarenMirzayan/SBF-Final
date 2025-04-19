package kz.kbtu.message_relay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.message_relay.model.OutboxEvent;
import kz.kbtu.message_relay.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxPoller {
    private static final Logger logger = LoggerFactory.getLogger(OutboxPoller.class);
    private static final String TOPIC = "order-events";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000) // Poll every 1 second
    @Transactional
    public void pollOutbox() {
        List<OutboxEvent> events = outboxRepository.findAllOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            logger.debug("No events found in outbox");
            return;
        }
        logger.info("Found {} events to process", events.size());
        for (OutboxEvent event : events) {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                logger.info("Processing outbox event with ID: {}", event.getId());
                kafkaTemplate.send(TOPIC, event.getAggregateId(), eventJson)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                logger.info("Published event {} to Kafka with offset {}",
                                        event.getId(), result.getRecordMetadata().offset());
                                // Delete the event after successful publishing
                                outboxRepository.delete(event);
                                logger.info("Deleted outbox event with ID: {}", event.getId());
                            } else {
                                logger.error("Failed to publish event {} to Kafka: {}", event.getId(), ex.getMessage());
                            }
                        });
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event {} to JSON: {}", event.getId(), e.getMessage());
            } catch (Exception e) {
                logger.error("Error processing event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}