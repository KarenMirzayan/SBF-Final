package kz.kbtu.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.kbtu.order_service.model.Order;
import kz.kbtu.order_service.model.OutboxEvent;
import kz.kbtu.order_service.repository.OrderRepository;
import kz.kbtu.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.RegEx;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public Order createOrder(Order order) {
        logger.info("Creating order: {}", order);
        Order newOrder = orderRepository.save(order);
        try {
            String payload = objectMapper.writeValueAsString(newOrder);
            OutboxEvent event = new OutboxEvent(
                    "Order",
                    newOrder.getId().toString(),
                    "ORDER_CREATED",
                    payload
            );
            logger.info("Persisting outbox event for ORDER_CREATED with ID: {}", event.getId());
            outboxRepository.save(event);
            logger.info("Order created successfully: {}", newOrder.getId());
            return newOrder;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order to JSON", e);
            throw new RuntimeException("Failed to serialize order to JSON", e);
        }
    }

    public Optional<Order> getOrderById(Long id) {
        logger.info("Fetching order by ID: {}", id);
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrder(Long id, Order updatedOrder) {
        logger.info("Updating order with ID: {}", id);
        Optional<Order> existingOrderOpt = orderRepository.findById(id);
        if (existingOrderOpt.isPresent()) {
            Order order = existingOrderOpt.get();
            order.setCustomerName(updatedOrder.getCustomerName());
            order.setTotalAmount(updatedOrder.getTotalAmount());
            order.setStatus(updatedOrder.getStatus());

            logger.info("Persisting updated order: {}", order.getId());
            Order savedOrder = orderRepository.save(order);

            try {
                String payload = objectMapper.writeValueAsString(savedOrder);
                OutboxEvent event = new OutboxEvent(
                        "Order",
                        id.toString(),
                        "ORDER_UPDATED",
                        payload
                );
                logger.info("Persisting outbox event for ORDER_UPDATED with ID: {}", event.getId());
                outboxRepository.save(event);
                logger.info("Order updated successfully: {}", savedOrder.getId());
                return savedOrder;
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize order to JSON", e);
                throw new RuntimeException("Failed to serialize order to JSON", e);
            }
        }
        logger.warn("Order not found: {}", id);
        throw new RuntimeException("Order not found");
    }

    @Transactional
    public void deleteOrder(Long id) {
        logger.info("Deleting order with ID: {}", id);
        if (orderRepository.existsById(id)) {
            OutboxEvent event = new OutboxEvent(
                    "Order",
                    id.toString(),
                    "ORDER_DELETED",
                    "{}"
            );
            logger.info("Persisting outbox event for ORDER_DELETED with ID: {}", event.getId());
            outboxRepository.save(event);
            outboxRepository.delete(event);
            logger.info("Order deleted successfully: {}", id);
        } else {
            logger.warn("Order not found: {}", id);
            throw new RuntimeException("Order not found");
        }
    }
}