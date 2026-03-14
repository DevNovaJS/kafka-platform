package com.custom.kafka.sample.payment;

public record PaymentEvent(
        String orderId,
        String userId,
        long amount,
        PaymentStatus status
) {
}
