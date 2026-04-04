package com.custom.kafka.dlt.e2e;

import com.custom.kafka.common.message.KafkaMessageHeaders;
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
class DltTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ObjectMapper objectMapper;

    static final String DLT_TOPIC = "activity-events-DLT";
    static final String PREFIX = "e2e-" + UUID.randomUUID().toString().substring(0, 8);
    static final String EVENT_KEY = PREFIX + "-dlt";
    static final String EVENT_ID = PREFIX + "-dlt-id";

    @Test
    @Order(1)
    @DisplayName("DLT 토픽 메시지 수신 → kafka_dlt_message 저장 검증")
    void dltMessageStored() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "eventKey", EVENT_KEY,
                "eventId", EVENT_ID,
                "payload", Map.of(
                        "userId", "user-e2e",
                        "sessionId", "sess-e2e",
                        "activityType", "CLICK",
                        "targetId", "btn-dlt-001",
                        "metadata", Map.of()
                )
        ));

        var record = new ProducerRecord<>(DLT_TOPIC, null, EVENT_KEY, json);
        record.headers().add(new RecordHeader(KafkaMessageHeaders.EVENT_KEY, EVENT_KEY.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.EVENT_ID, EVENT_ID.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.ORIGINAL_TOPIC, "activity-events".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.SERVICE_NAME, "e2e-test".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.DOMAIN, "activity".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaMessageHeaders.FAIL_COUNT, "1".getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Query query = Query.query(Criteria.where("eventKey").is(EVENT_KEY).and("eventId").is(EVENT_ID));
                    assertThat(mongoTemplate.count(query, "kafka_dlt_message")).isGreaterThan(0);
                });

        Query query = Query.query(Criteria.where("eventKey").is(EVENT_KEY).and("eventId").is(EVENT_ID));
        Document dltDoc = mongoTemplate.findOne(query, Document.class, "kafka_dlt_message");

        assertThat(dltDoc).isNotNull();
        assertThat(dltDoc.getString("originalTopic")).isEqualTo("activity-events");
        assertThat(dltDoc.getString("serviceName")).isEqualTo("e2e-test");
        assertThat(dltDoc.getString("domain")).isEqualTo("activity");
    }
}
