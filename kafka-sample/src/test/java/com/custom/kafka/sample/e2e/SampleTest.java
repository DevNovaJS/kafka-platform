package com.custom.kafka.sample.e2e;

import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.sample.activity.ActivityType;
import com.custom.kafka.sample.activity.UserActivityEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SampleTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ObjectMapper objectMapper;

    static final String TOPIC = "activity-events";
    static final String PREFIX = "e2e-" + UUID.randomUUID().toString().substring(0, 8);
    static final String NORMAL_EVENT_KEY = PREFIX + "-normal";
    static final String NORMAL_EVENT_ID = PREFIX + "-normal-id";
    static final String DLT_EVENT_KEY = PREFIX + "-dlt";
    static final String DLT_EVENT_ID = PREFIX + "-dlt-id";

    @Test
    @Order(1)
    @DisplayName("PAGE_VIEW 발행 → SUCCESS + activity log/stats 생성")
    void normalFlow() throws Exception {
        publish(NORMAL_EVENT_KEY, NORMAL_EVENT_ID, ActivityType.PAGE_VIEW, "page-001");

        await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(
                        countHistory(NORMAL_EVENT_KEY, NORMAL_EVENT_ID, "SUCCESS")).isGreaterThan(0));

        assertThat(countDocuments("user_activity_logs",
                new Document("eventKey", NORMAL_EVENT_KEY).append("eventId", NORMAL_EVENT_ID)))
                .isGreaterThan(0);

        assertThat(countDocuments("user_activity_stats",
                new Document("activityType", "PAGE_VIEW").append("targetId", "page-001")))
                .isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("동일 메시지 재발행 → SKIPPED + 중복 없음")
    void idempotency() throws Exception {
        publish(NORMAL_EVENT_KEY, NORMAL_EVENT_ID, ActivityType.PAGE_VIEW, "page-001");

        await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(
                        countHistory(NORMAL_EVENT_KEY, NORMAL_EVENT_ID, "SKIPPED")).isGreaterThan(0));

        assertThat(countDocuments("user_activity_logs",
                new Document("eventKey", NORMAL_EVENT_KEY).append("eventId", NORMAL_EVENT_ID)))
                .isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("CLICK 발행 → FAILED 처리 + DLT 토픽 발송")
    void clickFail() throws Exception {
        publish(DLT_EVENT_KEY, DLT_EVENT_ID, ActivityType.CLICK, "btn-001");

        await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(
                        countHistory(DLT_EVENT_KEY, DLT_EVENT_ID, "FAILED")).isGreaterThan(0));
    }

    private void publish(String eventKey, String eventId, ActivityType type, String targetId) throws Exception {
        var event = new UserActivityEvent("user-e2e", "sess-e2e", type, targetId, Map.of());
        var message = new KafkaEventMessage<>(eventKey, eventId, event);
        String json = objectMapper.writeValueAsString(message);

        var record = new ProducerRecord<>(TOPIC, null, eventKey, json);
        record.headers().add(new RecordHeader(KafkaMessageHeaders.SERVICE_NAME, "e2e-test".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.DOMAIN, "activity".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.FAIL_COUNT, "0".getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
    }

    private long countHistory(String eventKey, String eventId, String status) {
        return mongoTemplate.count(
                Query.query(Criteria.where("eventKey").is(eventKey)
                        .and("eventId").is(eventId)
                        .and("status").is(status)),
                "kafka_message_history");
    }

    private long countDocuments(String collection, Document filter) {
        Query query = new Query();
        filter.forEach((key, value) -> query.addCriteria(Criteria.where(key).is(value)));
        return mongoTemplate.count(query, collection);
    }
}
