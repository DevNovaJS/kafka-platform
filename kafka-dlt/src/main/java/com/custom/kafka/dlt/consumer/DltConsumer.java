package com.custom.kafka.dlt.consumer;

import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.dlt.document.DltMessage;
import com.custom.kafka.dlt.document.DltMessageStatus;
import com.custom.kafka.dlt.repository.DltMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DltConsumer {
    private final DltMessageRepository dltMessageRepository;
    @Value("${kafka.dlt.max-retry-count:3}")
    private int maxRetryCount;

    @KafkaListener(
            topicPattern = ".*\\.DLT",
            groupId = "${spring.kafka.consumer.group-id:kafka-dlt-group}",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, String> record,
            @Header(KafkaMessageHeaders.ORIGINAL_TOPIC) String originalTopic
    ) {
        String messageId = KafkaMessageHeaders.getMessageId(record).orElse("unknown");

        DltMessage dltMessage = dltMessageRepository.findByMessageId(messageId)
                .orElseGet(() -> DltMessage.of(messageId, originalTopic, record));

        dltMessage.incrementFailCount();
        dltMessage.updateStatus(this.resolveStatus(dltMessage.getFailCount()));

        dltMessageRepository.save(dltMessage);
    }

    private DltMessageStatus resolveStatus(int failCount) {
        return (failCount >= maxRetryCount)
                ? DltMessageStatus.PERMANENTLY_FAILED
                : DltMessageStatus.PENDING;
    }
}
