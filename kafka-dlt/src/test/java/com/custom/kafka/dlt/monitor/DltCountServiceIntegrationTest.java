package com.custom.kafka.dlt.monitor;

import com.custom.kafka.dlt.document.DltMessage;
import com.custom.kafka.dlt.document.DltMessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(DltCountService.class)
class DltCountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DltCountService dltCountService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("kafka_dlt_message");
    }

    @Test
    void countByOriginalTopicAfter_returnsCorrectCountPerTopic() {
        Instant windowStart = Instant.now().minusSeconds(3600);
        Instant withinWindow = Instant.now().minusSeconds(1800);

        mongoTemplate.insert(dltMessage("order", withinWindow), "kafka_dlt_message");
        mongoTemplate.insert(dltMessage("order", withinWindow), "kafka_dlt_message");
        mongoTemplate.insert(dltMessage("payment", withinWindow), "kafka_dlt_message");

        Map<String, Long> result = dltCountService.countByOriginalTopicAfter(windowStart);

        assertThat(result).containsEntry("order", 2L).containsEntry("payment", 1L);
    }

    @Test
    void countByOriginalTopicAfter_excludesMessagesOutsideWindow() {
        Instant windowStart = Instant.now().minusSeconds(3600);
        Instant withinWindow = Instant.now().minusSeconds(1800);
        Instant outsideWindow = Instant.now().minusSeconds(7200);

        mongoTemplate.insert(dltMessage("order", withinWindow), "kafka_dlt_message");
        mongoTemplate.insert(dltMessage("order", outsideWindow), "kafka_dlt_message");

        Map<String, Long> result = dltCountService.countByOriginalTopicAfter(windowStart);

        assertThat(result).containsEntry("order", 1L);
    }

    @Test
    void countByOriginalTopicAfter_emptyCollection_returnsEmptyMap() {
        Map<String, Long> result = dltCountService.countByOriginalTopicAfter(Instant.now().minusSeconds(3600));

        assertThat(result).isEmpty();
    }

    private DltMessage dltMessage(String originalTopic, Instant receivedAt) {
        return DltMessage.builder()
                .eventKey(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .originalTopic(originalTopic)
                .status(DltMessageStatus.PENDING)
                .receivedAt(receivedAt)
                .build();
    }
}
