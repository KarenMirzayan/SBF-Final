package kz.kbtu.message_relay.service;

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

    public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollOutbox() {
        List<OutboxEvent> events = outboxRepository.findAllOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            try {
                logger.info("Processing outbox event with ID: {}", event.getId());
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                logger.info("Published event {} to Kafka with offset {}",
                                        event.getId(), result.getRecordMetadata().offset());
                                outboxRepository.delete(event);
                                logger.info("Deleted outbox event with ID: {}", event.getId());
                            } else {
                                logger.error("Failed to publish event {} to Kafka: {}", event.getId(), ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                logger.error("Error processing event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}