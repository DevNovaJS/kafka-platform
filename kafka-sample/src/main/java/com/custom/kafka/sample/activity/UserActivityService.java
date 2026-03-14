package com.custom.kafka.sample.activity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

@Service
@Validated
@RequiredArgsConstructor
public class UserActivityService {
    private final MongoTemplate mongoTemplate;

    public void process(@Valid UserActivityEvent event, String messageId) {
        Instant now = Instant.now();

        mongoTemplate.upsert(
                Query.query(Criteria.where("messageId").is(messageId)),
                new Update()
                        .setOnInsert("messageId", messageId)
                        .setOnInsert("userId", event.userId())
                        .setOnInsert("sessionId", event.sessionId())
                        .setOnInsert("activityType", event.activityType())
                        .setOnInsert("targetId", event.targetId())
                        .setOnInsert("metadata", event.metadata())
                        .setOnInsert("occurredAt", now),
                UserActivityLog.class
        );

        mongoTemplate.upsert(
                Query.query(Criteria.where("activityType").is(event.activityType())
                        .and("targetId").is(event.targetId())),
                new Update()
                        .setOnInsert("activityType", event.activityType())
                        .setOnInsert("targetId", event.targetId())
                        .inc("count", 1)
                        .set("lastOccurredAt", now),
                UserActivityStats.class
        );
    }
}
