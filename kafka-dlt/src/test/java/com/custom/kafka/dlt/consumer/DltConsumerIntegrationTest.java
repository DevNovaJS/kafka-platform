package com.custom.kafka.dlt.consumer;

import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryService;
import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.dlt.document.DltMessage;
import com.custom.kafka.dlt.document.DltMessageStatus;
import com.custom.kafka.dlt.repository.DltMessageRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-DLT"})
class DltConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private DltMessageRepository dltMessageRepository;

    @MockitoBean
    private MessageHistoryRepository messageHistoryRepository;

    @MockitoBean
    private MessageHistoryService messageHistoryService;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Test
    void kafkaMessage_dispatchedToConsumer_andSavesViaRepository() {
        when(dltMessageRepository.findByMessageId("msg-int-001")).thenReturn(Optional.empty());
        when(dltMessageRepository.save(any(DltMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ProducerRecord<String, String> record = new ProducerRecord<>("order-DLT", null, null, "{\"orderId\":1}");
        record.headers().add(new RecordHeader(
                KafkaMessageHeaders.MESSAGE_ID, "msg-int-001".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(
                KafkaMessageHeaders.ORIGINAL_TOPIC, "order".getBytes(StandardCharsets.UTF_8)));

        kafkaTemplate.send(record);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ArgumentCaptor<DltMessage> captor = ArgumentCaptor.forClass(DltMessage.class);
            verify(dltMessageRepository, atLeastOnce()).save(captor.capture());

            DltMessage saved = captor.getValue();
            assertThat(saved.getMessageId()).isEqualTo("msg-int-001");
            assertThat(saved.getOriginalTopic()).isEqualTo("order");
            assertThat(saved.getPayload()).isEqualTo("{\"orderId\":1}");
            assertThat(saved.getFailCount()).isEqualTo(1);
            assertThat(saved.getStatus()).isEqualTo(DltMessageStatus.PENDING);
        });
    }
}
