package kz.kbtu.order_service;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class OrderControllerSimulation extends Simulation {

    // Base URL configuration - change this to match your environment
    private String baseUrl = "http://localhost:8080";

    // HTTP configuration
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/Performance Test");

    // Sample order payload
    private String orderJsonBody = "{\n" +
            "  \"customerName\": \"Test Customer\",\n" +
            "  \"totalAmount\": 99.99,\n" +
            "  \"orderDate\": \"2025-04-26T10:00:00\",\n" +
            "  \"status\": \"PROCESSING\"\n" +
            "}";

    // Define the scenario for creating orders
    private ScenarioBuilder createOrderScenario = scenario("Create Order Scenario")
            .exec(
                    http("Create Order Request")
                            .post("/api/orders")
                            .body(StringBody(orderJsonBody))
                            .asJson()
                            .check(status().is(200))
                            .check(jsonPath("$.id").saveAs("orderId"))
            )
            .pause(1)
            .exec(
                    http("Verify Created Order")
                            .get("/api/orders/#{orderId}")
                            .check(status().is(200))
                            .check(jsonPath("$.customerName").is("Test Customer"))
                            .check(jsonPath("$.status").is("PROCESSING"))
            );

    // Load simulation setup
    {
        setUp(
                createOrderScenario.injectOpen(
                        // Ramp up to 50 users over 30 seconds
                        rampUsers(50).during(Duration.ofSeconds(30)),

                        // Constant load of 50 users for 2 minutes
                        constantUsersPerSec(50).during(Duration.ofMinutes(2)),

                        // Ramp down to 0 users over 30 seconds
                        rampUsersPerSec(50).to(0).during(Duration.ofSeconds(30))
                ).protocols(httpProtocol)
        ).assertions(
                global().responseTime().max().lt(5000),      // Max response time under 5 seconds
                global().successfulRequests().percent().gt(95.0)  // At least 95% successful requests
        );
    }
}