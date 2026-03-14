package com.custom.kafka.sample.event;

public record ProductEvent(
        String productId,
        ProductAction action,
        int quantity
) {
}
