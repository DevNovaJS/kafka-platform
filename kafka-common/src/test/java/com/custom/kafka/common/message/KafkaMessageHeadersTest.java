package com.custom.kafka.common.message;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaMessageHeadersTest {

    @Test
    void getMessageId_present_returnsValue() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, "value");
        record.headers().add(new RecordHeader(KafkaMessageHeaders.MESSAGE_ID, "abc-123".getBytes(StandardCharsets.UTF_8)));

        Optional<String> result = KafkaMessageHeaders.getMessageId(record);

        assertThat(result).contains("abc-123");
    }

    @Test
    void getMessageId_absent_returnsEmpty() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, "value");

        Optional<String> result = KafkaMessageHeaders.getMessageId(record);

        assertThat(result).isEmpty();
    }

    @Test
    void getFailCount_present_returnsParsedInt() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, "value");
        record.headers().add(new RecordHeader(KafkaMessageHeaders.FAIL_COUNT, "5".getBytes(StandardCharsets.UTF_8)));

        int result = KafkaMessageHeaders.getFailCount(record);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void getFailCount_absent_returnsZero() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, "value");

        int result = KafkaMessageHeaders.getFailCount(record);

        assertThat(result).isZero();
    }

    @Test
    void getFailCount_invalidNumber_throws() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, "value");
        record.headers().add(new RecordHeader(KafkaMessageHeaders.FAIL_COUNT, "abc".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> KafkaMessageHeaders.getFailCount(record))
                .isInstanceOf(NumberFormatException.class);
    }
}
