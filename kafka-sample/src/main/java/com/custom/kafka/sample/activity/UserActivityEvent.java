package com.custom.kafka.sample.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UserActivityEvent(
        @NotBlank String userId,
        @NotBlank String sessionId,
        @NotNull ActivityType activityType,
        @NotBlank String targetId,
        Map<String, String> metadata
) {}
