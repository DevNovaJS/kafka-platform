package com.custom.kafka.dlt.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("kafka_dlt_message")
@CompoundIndexes({
        @CompoundIndex(def = "{'eventKey': 1, 'eventId': 1}", unique = true),
        @CompoundIndex(def = "{'serviceName': 1, 'domain': 1}")
})
public class DltMessage {
    @Id
    private String id;
    private String eventKey;
    private String eventId;
    @Indexed
    private String originalTopic;
    private String dltTopic;
    private int partition;
    private long offset;
    private String payload;
    private int failCount;
    @Indexed
    private DltMessageStatus status;
    private String serviceName;
    private String domain;
    private Instant receivedAt;

    public static DltMessage of(
            String eventKey,
            String eventId,
            String serviceName,
            String domain,
            String originalTopic,
            ConsumerRecord<String, String> record
    ) {
        return DltMessage.builder()
                .eventKey(eventKey)
                .eventId(eventId)
                .serviceName(serviceName)
                .domain(domain)
                .originalTopic(originalTopic)
                .dltTopic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .payload(record.value())
                .status(DltMessageStatus.PENDING)
                .receivedAt(Instant.now())
                .build();
    }

    public void incrementFailCount() {
        this.failCount++;
    }

    public void updateStatus(DltMessageStatus status) {
        this.status = status;
    }
}
