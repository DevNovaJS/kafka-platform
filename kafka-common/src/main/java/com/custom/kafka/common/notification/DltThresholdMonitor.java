package com.custom.kafka.common.notification;

import com.custom.kafka.common.history.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DltThresholdMonitor {
    private final MessageHistoryService historyService;
    private final SlackNotifier slackNotifier;
    @Value("${kafka.dlt.threshold.window-minutes:60}")
    private long windowMinutes;
    @Value("${kafka.dlt.threshold.max-count:10}")
    private long maxCount;

    @Scheduled(fixedRateString = "${kafka.dlt.threshold.check-interval:60000}")
    public void checkThreshold() {
        try {
            Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
            Map<String, Long> failedByTopic = historyService.countFailedByTopicAfter(windowStart);

            failedByTopic.forEach((topic, count) -> {
                if (count > maxCount) {
                    log.warn("DLT 임계치 초과: topic={}, count={}, window={}분", topic, count, windowMinutes);
                    slackNotifier.sendDltThresholdAlert(topic, count, windowMinutes);
                }
            });
        } catch (Exception e) {
            log.error("DLT 임계치 모니터링 실패: {}", e.getMessage(), e);
        }
    }
}
