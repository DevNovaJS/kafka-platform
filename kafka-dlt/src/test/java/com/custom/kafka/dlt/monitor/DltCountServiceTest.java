package com.custom.kafka.dlt.monitor;

import com.custom.kafka.dlt.model.TopicCount;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltCountServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AggregationResults<TopicCount> aggregationResults;

    @InjectMocks
    private DltCountService dltCountService;

    @Test
    void countByOriginalTopicAfter_returnsDltCountMap() {
        // given
        Instant windowStart = Instant.now().minusSeconds(3600);
        List<TopicCount> results = List.of(
                new TopicCount("order-events", 5),
                new TopicCount("payment-events", 3)
        );
        when(aggregationResults.getMappedResults()).thenReturn(results);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("kafka_dlt_message"), eq(TopicCount.class)))
                .thenReturn(aggregationResults);

        // when
        Map<String, Long> result = dltCountService.countByOriginalTopicAfter(windowStart);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("order-events", 5L);
        assertThat(result).containsEntry("payment-events", 3L);
    }

    @Test
    void countByOriginalTopicAfter_emptyResult_returnsEmptyMap() {
        // given
        Instant windowStart = Instant.now().minusSeconds(3600);
        when(aggregationResults.getMappedResults()).thenReturn(List.of());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("kafka_dlt_message"), eq(TopicCount.class)))
                .thenReturn(aggregationResults);

        // when
        Map<String, Long> result = dltCountService.countByOriginalTopicAfter(windowStart);

        // then
        assertThat(result).isEmpty();
    }
}
