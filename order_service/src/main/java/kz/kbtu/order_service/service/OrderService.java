package kz.kbtu.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kbtu.order_service.model.Order;
import kz.kbtu.order_service.model.OutboxEvent;
import kz.kbtu.order_service.repository.OrderRepository;
import kz.kbtu.order_service.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxRepository outboxEventRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(Order order) {
        Order newOrder = orderRepository.saveAndFlush(order);
        try {
            String payload = objectMapper.writeValueAsString(newOrder);
            OutboxEvent event = new OutboxEvent(
                    "Order",
                    newOrder.getId().toString(),
                    "ORDER_CREATED",
                    payload
            );
            outboxEventRepository.saveAndFlush(event);
            return newOrder;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order to JSON", e);
        }
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrder(Long id, Order updatedOrder) {
        Optional<Order> existingOrderOpt = orderRepository.findById(id);
        if (existingOrderOpt.isPresent()) {
            Order order = existingOrderOpt.get();
            order.setCustomerName(updatedOrder.getCustomerName());
            order.setTotalAmount(updatedOrder.getTotalAmount());
            order.setStatus(updatedOrder.getStatus());

            Order savedOrder = orderRepository.saveAndFlush(order);

            try {
                String payload = objectMapper.writeValueAsString(savedOrder);
                OutboxEvent event = new OutboxEvent(
                        "Order",
                        id.toString(),
                        "ORDER_UPDATED",
                        payload
                );
                outboxEventRepository.saveAndFlush(event);
                return savedOrder;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize order to JSON", e);
            }
        }
        throw new RuntimeException("Order not found");
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (orderRepository.existsById(id)) {
            OutboxEvent event = new OutboxEvent(
                    "Order",
                    id.toString(),
                    "ORDER_DELETED",
                    "{}"
            );
            outboxEventRepository.saveAndFlush(event);
            orderRepository.deleteById(id);
            orderRepository.flush();
        } else {
            throw new RuntimeException("Order not found");
        }
    }
}