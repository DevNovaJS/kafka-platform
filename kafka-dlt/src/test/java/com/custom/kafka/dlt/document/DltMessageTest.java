package com.custom.kafka.dlt.document;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class DltMessageTest {

    @Test
    void of_createsFromConsumerRecord() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-DLT", 2, 100L, "key1", "{\"data\":\"value\"}");

        DltMessage message = DltMessage.of("key-001", "id-001", "order-service", "commerce", "order", record);

        assertThat(message.getEventKey()).isEqualTo("key-001");
        assertThat(message.getEventId()).isEqualTo("id-001");
        assertThat(message.getServiceName()).isEqualTo("order-service");
        assertThat(message.getDomain()).isEqualTo("commerce");
        assertThat(message.getOriginalTopic()).isEqualTo("order");
        assertThat(message.getDltTopic()).isEqualTo("order-DLT");
        assertThat(message.getPartition()).isEqualTo(2);
        assertThat(message.getOffset()).isEqualTo(100L);
        assertThat(message.getPayload()).isEqualTo("{\"data\":\"value\"}");
        assertThat(message.getReceivedAt()).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS));
        assertThat(message.getFailCount()).isZero();
        assertThat(message.getStatus()).isNull();
    }

    @Test
    void incrementFailCount_incrementsByOne() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-DLT", 0, 0L, null, "payload");
        DltMessage message = DltMessage.of("key-001", "id-001", "svc", "domain", "order", record);

        assertThat(message.getFailCount()).isZero();

        message.incrementFailCount();
        assertThat(message.getFailCount()).isEqualTo(1);

        message.incrementFailCount();
        assertThat(message.getFailCount()).isEqualTo(2);
    }

    @Test
    void updateStatus_setsCorrectly() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-DLT", 0, 0L, null, "payload");
        DltMessage message = DltMessage.of("key-001", "id-001", "svc", "domain", "order", record);

        assertThat(message.getStatus()).isNull();

        message.updateStatus(DltMessageStatus.PENDING);
        assertThat(message.getStatus()).isEqualTo(DltMessageStatus.PENDING);

        message.updateStatus(DltMessageStatus.PERMANENTLY_FAILED);
        assertThat(message.getStatus()).isEqualTo(DltMessageStatus.PERMANENTLY_FAILED);
    }
}
