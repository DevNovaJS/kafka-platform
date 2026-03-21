package com.custom.kafka.common.processor;

import com.custom.kafka.common.dlt.DltSender;
import com.custom.kafka.common.history.MessageHistory;
import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryStatus;
import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.common.notification.SlackNotifier;
import com.custom.kafka.common.registry.MetadataRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class KafkaMessageProcessingAspect {
    private final MessageHistoryRepository messageHistoryRepository;
    private final DltSender dltSender;
    private final SlackNotifier slackNotifier;
    private final ObjectMapper objectMapper;
    private final MetadataRegistryService metadataRegistryService;

    @Around(value = "@annotation(com.custom.kafka.common.processor.KafkaMessageHandler) && args(record)",
            argNames = "joinPoint,record")
    public Object around(ProceedingJoinPoint joinPoint, ConsumerRecord<String, String> record) throws Throwable {
        int failCount = KafkaMessageHeaders.getFailCount(record);
        String serviceName = KafkaMessageHeaders.getServiceName(record).orElse("unknown");
        String domain = KafkaMessageHeaders.getDomain(record).orElse("unknown");
        String eventKey = null;
        String eventId = null;

        MessageHistory.MessageHistoryBuilder historyBuilder = MessageHistory.builder()
                .failCount(failCount)
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .payload(record.value())
                .serviceName(serviceName)
                .domain(domain)
                .createdAt(Instant.now());

        try {
            KafkaEventMessage<?> eventMessage = objectMapper.readValue(record.value(), KafkaEventMessage.class);
            eventKey = eventMessage.eventKey();
            eventId = eventMessage.eventId();

            if (eventKey == null || eventId == null) {
                log.error("eventKey 또는 eventId가 null — 처리 스킵: topic={}, payload={}", record.topic(), record.value());
                return null;
            }

            historyBuilder.eventKey(eventKey).eventId(eventId);
            metadataRegistryService.registerIfNew(serviceName, domain);

            if (messageHistoryRepository.existsByEventKeyAndEventIdAndFailCount(eventKey, eventId, failCount)) {
                log.info("중복 메시지 스킵: eventKey={}, eventId={}, failCount={}", eventKey, eventId, failCount);

                messageHistoryRepository.save(
                        historyBuilder
                                .status(MessageHistoryStatus.SKIPPED)
                                .build()
                );
                return null;
            }

            Object result = joinPoint.proceed();

            messageHistoryRepository.save(
                    historyBuilder
                            .status(MessageHistoryStatus.SUCCESS)
                            .build()
            );

            return result;
        } catch (Exception e) {
            log.error(
                    "메시지 처리 실패: eventKey={}, eventId={}, topic={}, error={}",
                    eventKey,
                    eventId,
                    record.topic(),
                    e.getMessage(),
                    e
            );

            if (eventKey == null || eventId == null) {
                return null;
            }

            messageHistoryRepository.save(
                    historyBuilder
                            .status(MessageHistoryStatus.FAILED)
                            .errorMessage(e.getMessage())
                            .build()
            );

            dltSender.send(eventKey, eventId, failCount, record);
            slackNotifier.sendError(eventKey, eventId, failCount, record, e);

            return null;
        }
    }
}
