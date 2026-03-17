package com.custom.kafka.common.history;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageHistoryServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AggregationResults<TopicCount> aggregationResults;

    @InjectMocks
    private MessageHistoryService messageHistoryService;

    @Test
    void countFailedByTopicAfter_returnsTopicCountMap() {
        // given
        Instant windowStart = Instant.now().minusSeconds(3600);
        List<TopicCount> topicCounts = List.of(
                new TopicCount("order-events", 5),
                new TopicCount("payment-events", 3)
        );
        when(aggregationResults.getMappedResults()).thenReturn(topicCounts);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("kafka_message_history"), eq(TopicCount.class)))
                .thenReturn(aggregationResults);

        // when
        Map<String, Long> result = messageHistoryService.countFailedByTopicAfter(windowStart);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("order-events", 5L);
        assertThat(result).containsEntry("payment-events", 3L);
    }

    @Test
    void countFailedByTopicAfter_emptyResult_returnsEmptyMap() {
        // given
        Instant windowStart = Instant.now().minusSeconds(3600);
        when(aggregationResults.getMappedResults()).thenReturn(List.of());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("kafka_message_history"), eq(TopicCount.class)))
                .thenReturn(aggregationResults);

        // when
        Map<String, Long> result = messageHistoryService.countFailedByTopicAfter(windowStart);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void countFailedByTopicAfter_passesCorrectCollectionName() {
        // given
        Instant windowStart = Instant.now();
        when(aggregationResults.getMappedResults()).thenReturn(List.of());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("kafka_message_history"), eq(TopicCount.class)))
                .thenReturn(aggregationResults);

        // when
        messageHistoryService.countFailedByTopicAfter(windowStart);

        // then
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("kafka_message_history"), eq(TopicCount.class));
    }
}
