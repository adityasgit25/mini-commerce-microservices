package com.aditya.common.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * As redis serializes the data before storing in it, so implementing this via Serializable.
 * You can check time in the postman how quickly the data is being retrieved via redis now,
 * before when the db gets hit, it was 50ms, and now its between 5ms-8ms.
 *
 * And by default it using cache-aside caching strategy.
 *
 * Now the thing is that redis instance can be connected to all the services, but all the services have its
 * own data, so its like that but in some cases we want it common like session management through redis,
 *
 * It all depends on what kinda architecture you build.
 *
 * Also one point, if the product service has many instances, then all that instances share the same
 * redis cache thing, and hence that's why redis is called distributed cache. Without it all the instances have
 * their own cache which is of no use(inconsistent data) right(via Caffeine).
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse implements Serializable {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;

}