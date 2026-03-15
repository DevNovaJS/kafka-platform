package com.custom.kafka.common.dlt;

import com.custom.kafka.common.message.KafkaMessageHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DltSenderTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private DltSender dltSender;

    @Test
    void send_buildsDltTopicName() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order", 0, 0L, "key", "payload");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));

        dltSender.send("msg-1", 0, record);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().topic()).isEqualTo("order-DLT");
    }

    @Test
    void send_setsHeadersCorrectly() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order", 0, 0L, "key", "payload");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));

        dltSender.send("msg-1", 2, record);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> sent = captor.getValue();

        assertThat(headerValue(sent, KafkaMessageHeaders.MESSAGE_ID)).isEqualTo("msg-1");
        assertThat(headerValue(sent, KafkaMessageHeaders.FAIL_COUNT)).isEqualTo("3");
        assertThat(headerValue(sent, KafkaMessageHeaders.ORIGINAL_TOPIC)).isEqualTo("order");
    }

    @Test
    void send_preservesPayload() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order", 0, 0L, "key", "{\"orderId\":123}");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));

        dltSender.send("msg-1", 0, record);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().value()).isEqualTo("{\"orderId\":123}");
    }

    private String headerValue(ProducerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
