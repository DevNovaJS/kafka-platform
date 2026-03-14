package com.custom.kafka.common.processor;

import com.custom.kafka.common.dlt.DltSender;
import com.custom.kafka.common.history.MessageHistory;
import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryStatus;
import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.common.notification.SlackNotifier;
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

    @Around(value = "@annotation(com.custom.kafka.common.processor.KafkaMessageHandler) && args(record)",
            argNames = "joinPoint,record")
    public Object around(ProceedingJoinPoint joinPoint, ConsumerRecord<String, String> record) throws Throwable {
        int failCount = KafkaMessageHeaders.getFailCount(record);
        String messageId = null;

        MessageHistory.MessageHistoryBuilder historyBuilder = MessageHistory.builder()
                .failCount(failCount)
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .payload(record.value())
                .createdAt(Instant.now());

        try {
            messageId = objectMapper.readValue(record.value(), KafkaEventMessage.class).messageId();
            historyBuilder.messageId(messageId);

            if (messageHistoryRepository.existsByMessageIdAndFailCount(messageId, failCount)) {
                log.info("중복 메시지 스킵: messageId={}, failCount={}", messageId, failCount);

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
                    "메시지 처리 실패: messageId={}, topic={}, error={}",
                    messageId,
                    record.topic(),
                    e.getMessage(),
                    e
            );

            if (messageId != null) {
                messageHistoryRepository.save(
                        historyBuilder
                                .status(MessageHistoryStatus.FAILED)
                                .errorMessage(e.getMessage())
                                .build()
                );
                dltSender.send(messageId, failCount, record);
                slackNotifier.sendError(messageId, failCount, record, e);
            }

            return null;
        }
    }
}
