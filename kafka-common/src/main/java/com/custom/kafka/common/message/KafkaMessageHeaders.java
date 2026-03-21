package com.custom.kafka.common.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaMessageHeaders {
    public static final String EVENT_KEY = "X-Event-Key";
    public static final String EVENT_ID = "X-Event-Id";
    public static final String FAIL_COUNT = "X-Fail-Count";
    public static final String ORIGINAL_TOPIC = "X-Original-Topic";
    public static final String SERVICE_NAME = "X-Service-Name";
    public static final String DOMAIN = "X-Domain";

    public static Optional<String> getEventKey(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, EVENT_KEY);
    }

    public static Optional<String> getEventId(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, EVENT_ID);
    }

    public static int getFailCount(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, FAIL_COUNT)
                .map(Integer::parseInt)
                .orElse(0);
    }

    public static Optional<String> getServiceName(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, SERVICE_NAME);
    }

    public static Optional<String> getDomain(ConsumerRecord<?, ?> record) {
        return getHeaderValue(record, DOMAIN);
    }

    private static Optional<String> getHeaderValue(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) {
            return Optional.empty();
        }

        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
