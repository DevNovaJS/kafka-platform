package com.custom.kafka.common.message;

public record KafkaEventMessage<T>(
        String eventKey,
        String eventId,
        T payload
) {}
