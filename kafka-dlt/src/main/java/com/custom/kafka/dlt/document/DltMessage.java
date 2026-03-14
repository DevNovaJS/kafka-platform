package com.custom.kafka.dlt.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("kafka_dlt_message")
public class DltMessage {
    @Id
    private String id;
    @Indexed(unique = true)
    private String messageId;
    @Indexed
    private String originalTopic;
    private String dltTopic;
    private int partition;
    private long offset;
    private String payload;
    private int failCount;
    @Indexed
    private DltMessageStatus status;
    private Instant receivedAt;

    public static DltMessage of(String messageId, String originalTopic, ConsumerRecord<String, String> record) {
        return DltMessage.builder()
                .messageId(messageId)
                .originalTopic(originalTopic)
                .dltTopic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .payload(record.value())
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
