package com.custom.kafka.sample.consumer;

import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.processor.KafkaMessageHandler;
import com.custom.kafka.sample.activity.UserActivityEvent;
import com.custom.kafka.sample.activity.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityConsumer {
    private final ObjectMapper objectMapper;
    private final UserActivityService userActivityService;

    @KafkaListener(
            topics = "activity-events",
            groupId = "${kafka.consumer.activity.group-id:kafka-sample-activity-group}",
            containerFactory = "activityKafkaListenerContainerFactory"
    )
    @KafkaMessageHandler
    public void consume(ConsumerRecord<String, String> record) {
        KafkaEventMessage<UserActivityEvent> message = objectMapper.readValue(
                record.value(),
                objectMapper.getTypeFactory().constructParametricType(KafkaEventMessage.class, UserActivityEvent.class)
        );
        UserActivityEvent event = message.payload();

        log.info("사용자 활동 이벤트 처리: userId={}, sessionId={}, activityType={}, targetId={}",
                event.userId(),
                event.sessionId(),
                event.activityType(),
                event.targetId()
        );

        userActivityService.process(event, message.eventKey(), message.eventId());
    }
}
