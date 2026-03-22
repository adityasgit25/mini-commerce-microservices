package com.aditya.inventoryservice.kafka;

import com.aditya.common.dto.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryConsumer {

    /** This serialization and de-serialization happens when sends the data from producer and then when consumer is taking it. */
    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void consume(OrderCreatedEvent event) {
        System.out.println("📦 Inventory Service received event: " + event);
        // Simulate stock update
        System.out.println("Reducing stock for product: " + event.getProductId());
    }
}
