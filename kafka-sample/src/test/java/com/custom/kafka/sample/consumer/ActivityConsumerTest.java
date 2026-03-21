package com.custom.kafka.sample.consumer;

import com.custom.kafka.sample.activity.ActivityType;
import com.custom.kafka.sample.activity.UserActivityEvent;
import com.custom.kafka.sample.activity.UserActivityService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityConsumerTest {

    @Mock
    private UserActivityService userActivityService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void consume_deserializesEventAndDelegatesToService() {
        // given
        ActivityConsumer consumer = new ActivityConsumer(objectMapper, userActivityService);
        String payload = """
                {
                    "eventKey": "key-1",
                    "eventId": "id-1",
                    "payload": {
                        "userId": "user-123",
                        "sessionId": "sess-456",
                        "activityType": "PAGE_VIEW",
                        "targetId": "page-789",
                        "metadata": {"browser": "Chrome"}
                    }
                }
                """;
        ConsumerRecord<String, String> record = createRecord(payload);

        // when
        consumer.consume(record);

        // then
        ArgumentCaptor<UserActivityEvent> eventCaptor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityService).process(eventCaptor.capture(), eq("key-1"), eq("id-1"));

        UserActivityEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo("user-123");
        assertThat(event.sessionId()).isEqualTo("sess-456");
        assertThat(event.activityType()).isEqualTo(ActivityType.PAGE_VIEW);
        assertThat(event.targetId()).isEqualTo("page-789");
        assertThat(event.metadata()).containsEntry("browser", "Chrome");
    }

    @Test
    void consume_clickEvent_processesCorrectly() {
        // given
        ActivityConsumer consumer = new ActivityConsumer(objectMapper, userActivityService);
        String payload = """
                {
                    "eventKey": "key-2",
                    "eventId": "id-2",
                    "payload": {
                        "userId": "user-100",
                        "sessionId": "sess-200",
                        "activityType": "CLICK",
                        "targetId": "btn-submit",
                        "metadata": null
                    }
                }
                """;
        ConsumerRecord<String, String> record = createRecord(payload);

        // when
        consumer.consume(record);

        // then
        ArgumentCaptor<UserActivityEvent> eventCaptor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityService).process(eventCaptor.capture(), eq("key-2"), eq("id-2"));

        assertThat(eventCaptor.getValue().activityType()).isEqualTo(ActivityType.CLICK);
        assertThat(eventCaptor.getValue().targetId()).isEqualTo("btn-submit");
    }

    @Test
    void consume_invalidJson_throwsException() {
        // given
        ActivityConsumer consumer = new ActivityConsumer(objectMapper, userActivityService);
        ConsumerRecord<String, String> record = createRecord("not-json");

        // when & then
        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(Exception.class);
    }

    private ConsumerRecord<String, String> createRecord(String payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("activity-events", 0, 0L, "key", payload);
        record.headers().add(new RecordHeader("X-Event-Key", "key-test".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("X-Event-Id", "id-test".getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
