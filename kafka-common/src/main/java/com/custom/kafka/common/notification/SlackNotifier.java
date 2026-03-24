package com.custom.kafka.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static com.custom.kafka.common.message.CommonConstants.SLACK_TIME_FORMAT;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier {
    private final RestClient restClient;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    @Async
    public void sendDltThresholdAlert(String topic, long count, long windowMinutes) {
        if (webhookUrl.isBlank()) {
            log.warn("webhook url is blank");
            return;
        }

        try {
            String json = SlackBlockKitBuilder.build(
                    ":rotating_light: DLT 임계치 초과",
                    new String[][]{
                            {"Topic", topic},
                            {"DLT 건수", count + "건"},
                            {"기준 시간", windowMinutes + "분"},
                            {"Time", SLACK_TIME_FORMAT.format(Instant.now())}
                    },
                    "%d분 내 DLT 메시지가 %d건 발생하여 임계치를 초과했습니다.".formatted(windowMinutes, count),
                    null
            );

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Sending DLT threshold alert", e);
        }
    }

}
