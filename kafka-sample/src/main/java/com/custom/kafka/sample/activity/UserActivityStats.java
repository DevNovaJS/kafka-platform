package com.custom.kafka.sample.activity;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Builder
@Document("user_activity_stats")
@CompoundIndex(def = "{'activityType': 1, 'targetId': 1}", unique = true)
public record UserActivityStats(
        @Id String id,
        ActivityType activityType,
        String targetId,
        long count,
        Instant lastOccurredAt
) {}
