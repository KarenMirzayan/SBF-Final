package kz.kbtu.order_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.kbtu.order_service.model.Order;
import kz.kbtu.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller", description = "API for managing orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create a new order",
            description = "Creates a new order with the provided details")
    @ApiResponse(responseCode = "200", description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = Order.class)))
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @Operation(summary = "Get order by ID",
            description = "Returns order details for the specified ID")
    @ApiResponse(responseCode = "200", description = "Order found",
            content = @Content(schema = @Schema(implementation = Order.class)))
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all orders",
            description = "Returns a list of all orders in the system")
    @ApiResponse(responseCode = "200", description = "List of orders retrieved successfully")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @Operation(summary = "Update an order",
            description = "Updates an existing order with the provided details")
    @ApiResponse(responseCode = "200", description = "Order updated successfully")
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return ResponseEntity.ok(orderService.updateOrder(id, order));
    }

    @Operation(summary = "Delete an order",
            description = "Deletes the specified order from the system")
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}