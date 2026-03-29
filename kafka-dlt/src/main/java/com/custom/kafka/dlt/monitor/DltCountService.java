package com.custom.kafka.dlt.monitor;

import com.custom.kafka.dlt.model.TopicCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DltCountService {
    private final MongoTemplate mongoTemplate;

    public Map<String, Long> countByOriginalTopicAfter(Instant after) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("receivedAt").gt(after)),
                Aggregation.group("originalTopic").count().as("count")
        );

        return mongoTemplate.aggregate(aggregation, "kafka_dlt_message", TopicCount.class)
                .getMappedResults()
                .stream()
                .collect(Collectors.toMap(TopicCount::id, TopicCount::count, Long::sum));
    }
}
