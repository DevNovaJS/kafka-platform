package com.custom.kafka.common.history;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Builder
@Document("kafka_message_history")
@CompoundIndexes({
        @CompoundIndex(def = "{'eventKey': 1, 'eventId': 1, 'failCount': 1}"),
        @CompoundIndex(def = "{'eventKey': 1}"),
        @CompoundIndex(def = "{'serviceName': 1, 'domain': 1}")
})
public record MessageHistory(
        @Id String id,
        String eventKey,
        String eventId,
        int failCount,
        String topic,
        int partition,
        long offset,
        String payload,
        MessageHistoryStatus status,
        String errorMessage,
        String serviceName,
        String domain,
        Instant createdAt
) {
}
