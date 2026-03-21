package com.custom.kafka.sample.activity;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Builder
@Document("user_activity_logs")
@CompoundIndex(def = "{'eventKey': 1, 'eventId': 1}", unique = true)
public record UserActivityLog(
        @Id String id,
        String eventKey,
        String eventId,
        @Indexed String userId,
        String sessionId,
        ActivityType activityType,
        String targetId,
        Map<String, String> metadata,
        Instant occurredAt
) {}
