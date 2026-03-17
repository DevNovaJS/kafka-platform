package com.custom.kafka.common.processor;

import com.custom.kafka.common.dlt.DltSender;
import com.custom.kafka.common.history.MessageHistory;
import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryStatus;
import com.custom.kafka.common.notification.SlackNotifier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageProcessingAspectTest {

    @Mock
    private MessageHistoryRepository messageHistoryRepository;

    @Mock
    private DltSender dltSender;

    @Mock
    private SlackNotifier slackNotifier;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void duplicateMessage_savesSkippedAndReturnsNull() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper
        );
        ConsumerRecord<String, String> record = createRecord("msg-1", 0,
                "{\"messageId\":\"msg-1\",\"payload\":{}}");

        when(messageHistoryRepository.existsByMessageIdAndFailCount("msg-1", 0)).thenReturn(true);

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.SKIPPED);
        assertThat(captor.getValue().messageId()).isEqualTo("msg-1");
    }

    @Test
    void successfulProcessing_savesSuccessStatus() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper
        );
        ConsumerRecord<String, String> record = createRecord("msg-2", 0,
                "{\"messageId\":\"msg-2\",\"payload\":{}}");

        when(messageHistoryRepository.existsByMessageIdAndFailCount("msg-2", 0)).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isEqualTo("ok");

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.SUCCESS);
        assertThat(captor.getValue().messageId()).isEqualTo("msg-2");
    }

    @Test
    void failedProcessing_savesFailedAndSendsDltAndNotifiesSlack() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper
        );
        ConsumerRecord<String, String> record = createRecord("msg-3", 1,
                "{\"messageId\":\"msg-3\",\"payload\":{}}");
        RuntimeException error = new RuntimeException("processing error");

        when(messageHistoryRepository.existsByMessageIdAndFailCount("msg-3", 1)).thenReturn(false);
        when(joinPoint.proceed()).thenThrow(error);

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MessageHistoryStatus.FAILED);
        assertThat(captor.getValue().errorMessage()).isEqualTo("processing error");

        verify(dltSender).send("msg-3", 1, record);
        verify(slackNotifier).sendError(eq("msg-3"), eq(1), eq(record), eq(error));
    }

    @Test
    void failedProcessing_withNullMessageId_skipsDltAndSlack() throws Throwable {
        // given — invalid JSON payload, messageId extraction fails
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper
        );
        ConsumerRecord<String, String> record = createRecord(null, 0, "invalid-json");

        // when
        Object result = aspect.around(joinPoint, record);

        // then
        assertThat(result).isNull();
        verify(dltSender, never()).send(anyString(), anyInt(), any());
        verify(slackNotifier, never()).sendError(anyString(), anyInt(), any(), any());
    }

    @Test
    void withFailCount_extractsFromHeader() throws Throwable {
        // given
        KafkaMessageProcessingAspect aspect = new KafkaMessageProcessingAspect(
                messageHistoryRepository, dltSender, slackNotifier, objectMapper
        );
        ConsumerRecord<String, String> record = createRecord("msg-4", 2,
                "{\"messageId\":\"msg-4\",\"payload\":{}}");

        when(messageHistoryRepository.existsByMessageIdAndFailCount("msg-4", 2)).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        aspect.around(joinPoint, record);

        // then
        verify(messageHistoryRepository).existsByMessageIdAndFailCount("msg-4", 2);

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().failCount()).isEqualTo(2);
    }

    private ConsumerRecord<String, String> createRecord(String messageId, int failCount, String payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0L, "key", payload);
        if (messageId != null) {
            record.headers().add(new RecordHeader("X-Message-Id", messageId.getBytes(StandardCharsets.UTF_8)));
        }
        if (failCount > 0) {
            record.headers().add(new RecordHeader("X-Fail-Count",
                    String.valueOf(failCount).getBytes(StandardCharsets.UTF_8)));
        }
        return record;
    }
}
