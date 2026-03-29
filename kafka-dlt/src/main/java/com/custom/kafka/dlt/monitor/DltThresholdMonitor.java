package com.custom.kafka.dlt.monitor;

import com.custom.kafka.common.notification.SlackNotifier;
import com.custom.kafka.dlt.config.DltThresholdProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(DltThresholdProperties.class)
public class DltThresholdMonitor {
    private final DltCountService dltCountService;
    private final SlackNotifier slackNotifier;
    private final DltThresholdProperties properties;

    // 임계치 초과 상태가 지속되면 매 주기마다 알림 재발송 — 의도된 동작
    @Scheduled(fixedRateString = "${kafka.dlt.threshold.check-interval:60000}")
    public void checkThreshold() {
        try {
            Instant windowStart = Instant.now().minus(properties.windowMinutes(), ChronoUnit.MINUTES);
            Map<String, Long> dltByTopic = dltCountService.countByOriginalTopicAfter(windowStart);

            dltByTopic.forEach((topic, count) -> {
                long topicMaxCount = properties.resolveMaxCount(topic);

                if (count < topicMaxCount) {
                    return;
                }

                log.warn("DLT 임계치 초과: topic={}, count={}, threshold={}, window={}분",
                        topic, count, topicMaxCount, properties.windowMinutes());
                slackNotifier.sendDltThresholdAlert(topic, count, properties.windowMinutes(), topicMaxCount);
            });
        } catch (Exception e) {
            log.error("DLT 임계치 모니터링 실패: {}", e.getMessage(), e);
        }
    }
}
