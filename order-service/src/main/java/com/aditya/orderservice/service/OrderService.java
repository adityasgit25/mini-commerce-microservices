package com.aditya.orderservice.service;

import com.aditya.common.dto.OrderCreatedEvent;
import com.aditya.common.dto.ProductResponse;
import com.aditya.orderservice.client.ProductClient;
import com.aditya.orderservice.entity.Order;
import com.aditya.orderservice.kafka.OrderProducer;
import com.aditya.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderProducer orderProducer;

    @CircuitBreaker(name = "productService", fallbackMethod = "productFallback")
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        ProductResponse productResponse = productClient.getProduct(productId);

        BigDecimal totalPrice = productResponse.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), productId, quantity);

        orderProducer.sendOrderCreatedEvent(event);

        return order;
    }

    public Order productFallback(Long userId, Long productId, Integer quantity, Throwable throwable) {

        System.out.println("Product service unavailable, fallback triggered");

        System.out.println("🔥 REAL ERROR: " + throwable.getMessage());
        throwable.printStackTrace();   // 👈 VERY IMPORTANT

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .totalPrice(BigDecimal.ZERO)
                .build();

        return orderRepository.save(order);
    }

}
