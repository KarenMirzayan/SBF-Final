package kz.kbtu.order_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Order entity representing customer orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique order identifier")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Name of the customer who placed the order", example = "John Doe")
    private String customerName;

    @Column(nullable = false)
    @Schema(description = "Total amount of the order", example = "99.99")
    private Double totalAmount;

    @Column()
    @Schema(description = "Date and time when the order was placed",
            example = "2025-04-26T10:00:00")
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Current status of the order",
            example = "PROCESSING",
            allowableValues = {"PROCESSING", "DELIVERED", "CANCELLED"})
    private OrderStatus status;
}
