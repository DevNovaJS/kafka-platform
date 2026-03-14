package com.custom.kafka.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier {
    @Value("${slack.webhook.url:}")
    private String webhookUrl;
    private final RestClient restClient;

    public void sendError(String messageId, int failCount, ConsumerRecord<String, String> record, Exception e) {
        if (webhookUrl.isBlank()) {
            log.warn("Slack webhook URL 미설정 — 에러 알림 스킵: messageId={}", messageId);
            return;
        }

        String text = """
                *[Kafka 처리 오류]*
                • Topic: `%s` (partition=%d, offset=%d)
                • MessageId: `%s`
                • FailCount: %d
                • Error: %s
                """.formatted(
                record.topic(), record.partition(), record.offset(),
                messageId, failCount,
                e.getMessage()
        );

        send(text);
    }

    @Async
    public void sendDltThresholdAlert(String topic, long count, long windowMinutes) {
        if (webhookUrl.isBlank()) {
            log.warn("Slack webhook URL 미설정 — DLT 임계치 알림 스킵: topic={}", topic);
            return;
        }

        String text = """
                *[DLT 임계치 초과 경보]*
                • Topic: `%s`
                • %d분 내 DLT 건수: %d건
                """.formatted(topic, windowMinutes, count);

        send(text);
    }

    private void send(String text) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Slack 알림 발송 실패: {}", e.getMessage(), e);
        }
    }
}
