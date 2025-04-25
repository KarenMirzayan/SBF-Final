package kz.kbtu.message_relay.service;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OutboxPoller implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(OutboxPoller.class);
    private static final String NOTIFY_CHANNEL = "outbox_insert";
    private static final long FALLBACK_INTERVAL = 10000;

    private final OutboxProcessorService processorService;
    private final DataSource dataSource;
    private final ExecutorService executorService;
    private long lastFallbackCheck = 0;

    public OutboxPoller(OutboxProcessorService processorService, DataSource dataSource) {
        this.processorService = processorService;
        this.dataSource = dataSource;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Start the notification listener in a separate thread
        executorService.submit(this::listenForNotifications);
    }

    @Override
    public void destroy() throws Exception {
        // Shutdown the executor service
        executorService.shutdown();
    }

    private void listenForNotifications() {
        try (Connection connection = dataSource.getConnection()) {
            // Set up LISTEN
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("LISTEN " + NOTIFY_CHANNEL);
            }

            PGConnection pgConnection = connection.unwrap(PGConnection.class);

            // Periodically check for notifications and process events
            while (!Thread.currentThread().isInterrupted()) {
                // Poll notifications
                PGNotification[] notifications = pgConnection.getNotifications();
                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        String eventId = notification.getParameter();
                        logger.debug("Received notification for event ID: {}", eventId);
                        processorService.processEventById(UUID.fromString(eventId));
                    }
                }

                // Fallback: Periodically check for unprocessed events
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFallbackCheck >= FALLBACK_INTERVAL) {
                    processorService.processPendingEvents();
                    lastFallbackCheck = currentTime;
                }

                // Wait before next check to avoid busy loop
                Thread.sleep(1000); // Check every 1 second for notifications
            }
        } catch (Exception e) {
            logger.error("Error in notification listener: {}", e.getMessage(), e);
            // Restart listener after delay
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            listenForNotifications(); // Retry
        }
    }
}