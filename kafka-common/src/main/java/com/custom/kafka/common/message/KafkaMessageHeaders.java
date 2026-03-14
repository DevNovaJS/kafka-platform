package com.custom.kafka.common.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaMessageHeaders {
    public static final String MESSAGE_ID = "X-Message-Id";
    public static final String FAIL_COUNT = "X-Fail-Count";
    public static final String ORIGINAL_TOPIC = "X-Original-Topic";

    public static Optional<String> getMessageId(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, MESSAGE_ID);
    }

    public static int getFailCount(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, FAIL_COUNT)
                .map(Integer::parseInt)
                .orElse(0);
    }

    private static Optional<String> getHeaderValue(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) {
            return Optional.empty();
        }

        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
