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
            String json = buildBlockKit(
                    new String[][]{
                            {"Topic", topic},
                            {"DLT 건수", count + "건"},
                            {"기준 시간", windowMinutes + "분"},
                            {"Time", SLACK_TIME_FORMAT.format(Instant.now())}
                    },
                    "%d분 내 DLT 메시지가 %d건 발생하여 임계치를 초과했습니다.".formatted(windowMinutes, count)
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

    private String buildBlockKit(String[][] fields, String message) {
        var sb = new StringBuilder();
        sb.append("""
                {"blocks":[{"type":"header","text":{"type":"plain_text","text":"%s"}},""".formatted(escape(":rotating_light: DLT 임계치 초과")));

        // fields section
        sb.append("""
                {"type":"section","fields":[""");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("""
                    {"type":"mrkdwn","text":"*%s:*\\n%s"}""".formatted(
                    escape(fields[i][0]), escape(fields[i][1])));
        }
        sb.append("]}");

        // message section
        if (message != null && !message.isBlank()) {
            sb.append("""
                    ,{"type":"section","text":{"type":"mrkdwn","text":"*Message:*\\n```%s```"}}""".formatted(
                    escape(truncate(message))));
        }

        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "    ");
    }

    private static String truncate(String text) {
        int maxLength = 2500;

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "…";
    }
}
