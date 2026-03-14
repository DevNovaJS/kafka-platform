package com.custom.kafka.sample.event;

public record PaymentEvent(
        String orderId,
        String userId,
        long amount,
        PaymentStatus status
) {
}
