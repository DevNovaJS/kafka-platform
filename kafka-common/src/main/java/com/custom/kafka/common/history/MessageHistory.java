package com.custom.kafka.common.history;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Builder
@Document("kafka_message_history")
@CompoundIndex(def = "{'messageId': 1, 'failCount': 1}")
public record MessageHistory(
        @Id String id,
        String messageId,
        int failCount,
        String topic,
        int partition,
        long offset,
        String payload,
        MessageHistoryStatus status,
        String errorMessage,
        Instant createdAt
) {
}
