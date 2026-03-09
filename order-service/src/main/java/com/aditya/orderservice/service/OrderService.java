package com.aditya.orderservice.service;

import com.aditya.common.dto.ProductResponse;
import com.aditya.orderservice.entity.Order;
import com.aditya.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public Order createOrder(Long userId, Long productId, Integer quantity) {
        ProductResponse productResponse = restTemplate.getForObject(
                "http://localhost:8082/products/" + productId,
                ProductResponse.class
        );

        BigDecimal totalPrice = productResponse.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        return orderRepository.save(order);


    }

}
