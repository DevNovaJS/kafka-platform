package com.custom.kafka.common.dlt;

import com.custom.kafka.common.message.CommonConstants;
import com.custom.kafka.common.message.KafkaMessageHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class DltSender {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String messageId, int failCount, ConsumerRecord<String, String> record) {
        int nextFailCount = failCount + 1;
        String dltTopic = record.topic() + CommonConstants.DLT_TOPIC_SUFFIX;
        ProducerRecord<String, String> dltRecord = this.buildDltRecord(dltTopic, record, messageId, nextFailCount);

        kafkaTemplate.send(dltRecord)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        log.error("DLT 발송 실패: topic={}, messageId={}", dltTopic, messageId, ex);
                    }
                });
    }

    private ProducerRecord<String, String> buildDltRecord(
            String dltTopic,
            ConsumerRecord<String, String> origin,
            String messageId,
            int nextFailCount
    ) {
        ProducerRecord<String, String> record = new ProducerRecord<>(dltTopic, null, origin.key(), origin.value());
        record.headers().add(header(KafkaMessageHeaders.MESSAGE_ID, messageId));
        record.headers().add(header(KafkaMessageHeaders.FAIL_COUNT, String.valueOf(nextFailCount)));
        record.headers().add(header(KafkaMessageHeaders.ORIGINAL_TOPIC, origin.topic()));
        return record;
    }

    private RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
