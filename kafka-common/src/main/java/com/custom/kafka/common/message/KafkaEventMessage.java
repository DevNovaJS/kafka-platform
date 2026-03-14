package com.custom.kafka.common.message;

public record KafkaEventMessage<T>(
        String messageId,
        T payload
) {}
