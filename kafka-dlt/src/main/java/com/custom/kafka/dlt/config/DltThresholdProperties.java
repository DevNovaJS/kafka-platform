package com.custom.kafka.dlt.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "kafka.dlt.threshold")
public record DltThresholdProperties(
        @DefaultValue("60") @Min(1) long windowMinutes,
        @DefaultValue("10") @Min(10) long maxCount,
        @DefaultValue Map<String, TopicThreshold> topics  // 미설정 시 빈 맵 — 전역 기본값만 적용
) {
    public record TopicThreshold(@Min(10) Long maxCount) {}

    public long resolveMaxCount(String topic) {
        TopicThreshold override = topics.get(topic);

        if (override != null && override.maxCount() != null) {
            return override.maxCount();
        }

        return maxCount;
    }
}
