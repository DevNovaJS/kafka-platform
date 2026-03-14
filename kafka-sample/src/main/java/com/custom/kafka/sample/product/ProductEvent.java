package com.custom.kafka.sample.product;

public record ProductEvent(
        String productId,
        ProductAction action,
        int quantity
) {
}
