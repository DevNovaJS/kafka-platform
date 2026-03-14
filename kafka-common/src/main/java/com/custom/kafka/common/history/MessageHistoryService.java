package com.custom.kafka.common.history;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHistoryService {
    private final MongoTemplate mongoTemplate;

    public Map<String, Long> countFailedByTopicAfter(Instant after) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("status")
                                .is(MessageHistoryStatus.FAILED)
                                .and("createdAt")
                                .gt(after)
                ),
                Aggregation.group("topic").count().as("count")
        );

        return mongoTemplate.aggregate(aggregation, "kafka_message_history", TopicCount.class)
                .getMappedResults()
                .stream()
                .collect(Collectors.toMap(TopicCount::id, TopicCount::count));
    }

}
