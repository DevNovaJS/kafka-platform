package com.custom.kafka.dlt.consumer;

import com.custom.kafka.common.message.KafkaMessageHeaders;
import com.custom.kafka.dlt.document.DltMessage;
import com.custom.kafka.dlt.document.DltMessageStatus;
import com.custom.kafka.dlt.repository.DltMessageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DltConsumerTest {

    @Mock
    private DltMessageRepository dltMessageRepository;

    @InjectMocks
    private DltConsumer dltConsumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dltConsumer, "maxRetryCount", 3);
    }

    @Test
    void consume_newMessage_savesAsPending() {
        ConsumerRecord<String, String> record = createRecord("msg-001");
        when(dltMessageRepository.findByMessageId("msg-001")).thenReturn(Optional.empty());

        dltConsumer.consume(record, "order");

        ArgumentCaptor<DltMessage> captor = ArgumentCaptor.forClass(DltMessage.class);
        verify(dltMessageRepository).save(captor.capture());

        DltMessage saved = captor.getValue();
        assertThat(saved.getMessageId()).isEqualTo("msg-001");
        assertThat(saved.getFailCount()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(DltMessageStatus.PENDING);
    }

    @Test
    void consume_existingMessage_incrementsAndStaysPending() {
        ConsumerRecord<String, String> record = createRecord("msg-002");
        DltMessage existing = DltMessage.of("msg-002", "order", record);
        existing.incrementFailCount(); // failCount = 1
        existing.updateStatus(DltMessageStatus.PENDING);

        when(dltMessageRepository.findByMessageId("msg-002")).thenReturn(Optional.of(existing));

        dltConsumer.consume(record, "order");

        ArgumentCaptor<DltMessage> captor = ArgumentCaptor.forClass(DltMessage.class);
        verify(dltMessageRepository).save(captor.capture());

        DltMessage saved = captor.getValue();
        assertThat(saved.getFailCount()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(DltMessageStatus.PENDING);
    }

    @Test
    void consume_failCountReachesMax_permanentlyFailed() {
        ConsumerRecord<String, String> record = createRecord("msg-003");
        DltMessage existing = DltMessage.of("msg-003", "order", record);
        existing.incrementFailCount(); // 1
        existing.incrementFailCount(); // 2

        when(dltMessageRepository.findByMessageId("msg-003")).thenReturn(Optional.of(existing));

        dltConsumer.consume(record, "order");

        ArgumentCaptor<DltMessage> captor = ArgumentCaptor.forClass(DltMessage.class);
        verify(dltMessageRepository).save(captor.capture());

        DltMessage saved = captor.getValue();
        assertThat(saved.getFailCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(DltMessageStatus.PERMANENTLY_FAILED);
    }

    @Test
    void consume_failCountExceedsMax_permanentlyFailed() {
        ConsumerRecord<String, String> record = createRecord("msg-004");
        DltMessage existing = DltMessage.of("msg-004", "order", record);
        for (int i = 0; i < 5; i++) {
            existing.incrementFailCount();
        }

        when(dltMessageRepository.findByMessageId("msg-004")).thenReturn(Optional.of(existing));

        dltConsumer.consume(record, "order");

        ArgumentCaptor<DltMessage> captor = ArgumentCaptor.forClass(DltMessage.class);
        verify(dltMessageRepository).save(captor.capture());

        DltMessage saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DltMessageStatus.PERMANENTLY_FAILED);
    }

    @Test
    void consume_missingMessageIdHeader_usesUnknown() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-DLT", 0, 0L, null, "payload");
        // No X-Message-Id header added
        when(dltMessageRepository.findByMessageId("unknown")).thenReturn(Optional.empty());

        dltConsumer.consume(record, "order");

        verify(dltMessageRepository).findByMessageId("unknown");
        verify(dltMessageRepository).save(any(DltMessage.class));
    }

    private ConsumerRecord<String, String> createRecord(String messageId) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-DLT", 0, 0L, null, "{\"data\":\"test\"}");
        record.headers().add(new RecordHeader(
                KafkaMessageHeaders.MESSAGE_ID,
                messageId.getBytes(StandardCharsets.UTF_8)
        ));
        return record;
    }
}
