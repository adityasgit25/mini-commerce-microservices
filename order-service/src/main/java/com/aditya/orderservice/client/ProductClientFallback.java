package com.aditya.orderservice.client;

import com.aditya.common.dto.ProductResponse;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductResponse getProduct(Long id) {
        return ProductResponse.builder()
                .id(id)
                .name("Unavailable Product")
                .price(BigDecimal.ZERO)
                .stock(0)
                .build();
    }
}
